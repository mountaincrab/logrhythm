import { useCallback, useEffect, useRef, useState } from 'react'
import {
  collection, doc, query, where, onSnapshot, getDocs,
  setDoc, updateDoc, serverTimestamp, runTransaction,
} from 'firebase/firestore'
import { db } from '../firebase'
import { Medication, MedicationForm, MedicationSchedule, RepeatRule } from '../types'
import {
  doseMillis, isoDateString, localEpochDay, materialisedDoseId, medicationDose, scheduleOccursOn,
} from '../lib/medications'

/** How far back a first run (or a long absence) will fill in missed doses. */
const BACKFILL_DAYS = 14

function mapMedication(id: string, d: Record<string, unknown>): Medication {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    name: (d.name as string) ?? '',
    form: (d.form as MedicationForm) ?? 'TABLET',
    doseAmount: (d.doseAmount as string) ?? '',
    doseUnit: (d.doseUnit as string) ?? '',
    sortOrder: (d.sortOrder as number) ?? 0,
    createdAt: (d.createdAt as number) ?? 0,
    isDeleted: (d.isDeleted as boolean) ?? false,
  }
}

function mapSchedule(id: string, d: Record<string, unknown>): MedicationSchedule {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    medicationId: (d.medicationId as string) ?? '',
    quantity: (d.quantity as string) ?? '',
    timeMinutes: (d.timeMinutes as number) ?? 0,
    repeatRule: (d.repeatRule as RepeatRule) ?? 'DAILY',
    daysOfWeek: Array.isArray(d.daysOfWeek) ? (d.daysOfWeek as number[]).map(Number) : [],
    startEpochDay: (d.startEpochDay as number) ?? 0,
    isActive: (d.isActive as boolean) ?? true,
    createdAt: (d.createdAt as number) ?? 0,
    isDeleted: (d.isDeleted as boolean) ?? false,
  }
}

export interface MedicationInput {
  name: string
  form: MedicationForm
  doseAmount: string
  doseUnit: string
}

export interface ScheduleInput {
  medicationId: string
  quantity: string
  timeMinutes: number
  repeatRule: RepeatRule
  daysOfWeek: number[]
}

export function useMedications(userId: string, profileId: string) {
  const [medications, setMedications] = useState<Medication[]>([])
  const [schedules, setSchedules] = useState<MedicationSchedule[]>([])
  const [loading, setLoading] = useState(true)

  const col = useCallback(
    (name: string) => collection(db, 'users', userId, name),
    [userId],
  )

  useEffect(() => {
    setLoading(true)
    let medsReady = false
    let schedulesReady = false
    const markReady = () => { if (medsReady && schedulesReady) setLoading(false) }

    const unsubMeds = onSnapshot(query(col('medications')), (snap) => {
      setMedications(
        snap.docs
          .map((d) => mapMedication(d.id, d.data()))
          .filter((m) => !m.isDeleted && m.profileId === profileId)
          .sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)),
      )
      medsReady = true
      markReady()
    })
    const unsubSchedules = onSnapshot(query(col('medication_schedules')), (snap) => {
      setSchedules(
        snap.docs
          .map((d) => mapSchedule(d.id, d.data()))
          .filter((s) => !s.isDeleted && s.profileId === profileId)
          .sort((a, b) => a.timeMinutes - b.timeMinutes),
      )
      schedulesReady = true
      markReady()
    })
    return () => { unsubMeds(); unsubSchedules() }
  }, [col, profileId])

  // ── catalog ──────────────────────────────────────────────────────────────

  const addMedication = async (input: MedicationInput): Promise<string> => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('medications'), id), {
      userId, profileId,
      name: input.name.trim(),
      form: input.form,
      doseAmount: input.doseAmount.trim(),
      doseUnit: input.doseUnit.trim(),
      sortOrder: medications.length,
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
    return id
  }

  const updateMedication = async (id: string, input: MedicationInput) => {
    await updateDoc(doc(col('medications'), id), {
      name: input.name.trim(),
      form: input.form,
      doseAmount: input.doseAmount.trim(),
      doseUnit: input.doseUnit.trim(),
      updatedAt: serverTimestamp(),
    })
  }

  /** Deleting a medication retires its scheduled doses; recorded history is left intact. */
  const deleteMedication = async (id: string) => {
    await updateDoc(doc(col('medications'), id), { isDeleted: true, updatedAt: serverTimestamp() })
    await Promise.all(
      schedules
        .filter((s) => s.medicationId === id)
        .map((s) => updateDoc(doc(col('medication_schedules'), s.id), {
          isDeleted: true, updatedAt: serverTimestamp(),
        })),
    )
  }

  // ── schedule ─────────────────────────────────────────────────────────────

  const addSchedule = async (input: ScheduleInput) => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('medication_schedules'), id), {
      userId, profileId,
      medicationId: input.medicationId,
      quantity: input.quantity.trim(),
      timeMinutes: input.timeMinutes,
      repeatRule: input.repeatRule,
      daysOfWeek: [...input.daysOfWeek].sort((a, b) => a - b),
      startEpochDay: localEpochDay(new Date()),
      isActive: true,
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }

  const updateSchedule = async (id: string, input: ScheduleInput) => {
    await updateDoc(doc(col('medication_schedules'), id), {
      medicationId: input.medicationId,
      quantity: input.quantity.trim(),
      timeMinutes: input.timeMinutes,
      repeatRule: input.repeatRule,
      daysOfWeek: [...input.daysOfWeek].sort((a, b) => a - b),
      updatedAt: serverTimestamp(),
    })
  }

  const setScheduleActive = async (id: string, isActive: boolean) => {
    await updateDoc(doc(col('medication_schedules'), id), { isActive, updatedAt: serverTimestamp() })
  }

  const deleteSchedule = async (id: string) => {
    await updateDoc(doc(col('medication_schedules'), id), { isDeleted: true, updatedAt: serverTimestamp() })
  }

  // ── materialisation ──────────────────────────────────────────────────────

  /**
   * Turns scheduled doses whose time has passed into real medication_entries docs. This is
   * the schedule's entire job: automating the adding of dose entries.
   *
   * Only doses in the past are written, so the timeline never claims a dose that hasn't
   * happened yet. Ids are
   * derived from schedule + date so this and the Android app converge on one document, and
   * each create runs in a transaction that bails if the doc already exists — including when
   * the other device has already recorded it as taken, or the user deleted it.
   */
  const materialiseDueDoses = useCallback(async () => {
    const active = schedules.filter((s) => s.isActive)
    if (active.length === 0) return
    const byId = new Map(medications.map((m) => [m.id, m]))

    const now = Date.now()
    const today = new Date()
    const earliest = new Date(today)
    earliest.setDate(earliest.getDate() - BACKFILL_DAYS)
    earliest.setHours(0, 0, 0, 0)

    // One read to skip everything that already exists, so the transactions below only run
    // for genuinely new doses.
    const existing = new Set<string>()
    const snap = await getDocs(query(
      col('medication_entries'),
      where('occurredAt', '>=', earliest.getTime()),
    ))
    snap.docs.forEach((d) => existing.add(d.id))

    const pending: { id: string; data: Record<string, unknown> }[] = []
    for (const schedule of active) {
      // A schedule whose medication was deleted produces nothing.
      const medication = byId.get(schedule.medicationId)
      if (!medication) continue

      const start = new Date(Math.max(earliest.getTime(), schedule.startEpochDay * 86_400_000))
      for (const date of eachDay(start, today)) {
        if (!scheduleOccursOn(schedule.repeatRule, schedule.daysOfWeek, schedule.startEpochDay, date)) continue
        const dueAt = doseMillis(date, schedule.timeMinutes)
        if (dueAt > now) continue
        const id = materialisedDoseId(schedule.id, isoDateString(date))
        if (existing.has(id)) continue
        pending.push({
          id,
          data: {
            userId, profileId,
            medicationId: medication.id,
            medicationName: medication.name,
            dose: medicationDose(medication),
            quantity: schedule.quantity,
            occurredAt: dueAt,
            scheduleId: schedule.id,
            notes: null,
            createdAt: Date.now(),
            isDeleted: false,
          },
        })
      }
    }

    await Promise.all(pending.map(({ id, data }) =>
      runTransaction(db, async (tx) => {
        const ref = doc(col('medication_entries'), id)
        const snapshot = await tx.get(ref)
        if (snapshot.exists()) return
        tx.set(ref, { ...data, updatedAt: serverTimestamp() })
      }).catch(() => { /* another device won the race; its document stands */ }),
    ))
  }, [col, medications, schedules, userId, profileId])

  // Fill in anything that came due while the app was closed. Re-runs when the schedule
  // changes, and hourly so a dose materialises during a long-lived session too.
  const materialise = useRef(materialiseDueDoses)
  materialise.current = materialiseDueDoses
  useEffect(() => {
    if (loading) return
    materialise.current()
    const timer = setInterval(() => materialise.current(), 60 * 60 * 1000)
    return () => clearInterval(timer)
  }, [loading, schedules, medications])

  return {
    medications,
    schedules,
    loading,
    addMedication, updateMedication, deleteMedication,
    addSchedule, updateSchedule, setScheduleActive, deleteSchedule,
  }
}

/** Local dates from `from` to `to` inclusive. */
function eachDay(from: Date, to: Date): Date[] {
  const days: Date[] = []
  const cursor = new Date(from.getFullYear(), from.getMonth(), from.getDate())
  const last = new Date(to.getFullYear(), to.getMonth(), to.getDate())
  while (cursor <= last) {
    days.push(new Date(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return days
}
