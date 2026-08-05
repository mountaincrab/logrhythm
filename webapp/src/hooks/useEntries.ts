import { useEffect, useMemo, useState } from 'react'
import {
  collection, doc, query, onSnapshot,
  setDoc, updateDoc, serverTimestamp,
} from 'firebase/firestore'
import { db } from '../firebase'
import {
  PoopEntry, FoodEntry, NoteEntry, MealTag, TimelineEntry,
  MedicationEntry,
} from '../types'

export function mapPoop(id: string, d: Record<string, unknown>): PoopEntry {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    occurredAt: (d.occurredAt as number) ?? 0,
    bristolTypes: Array.isArray(d.bristolTypes) ? (d.bristolTypes as number[]).map(Number) : [],
    blood: (d.blood as number) ?? 1,
    notes: (d.notes as string) ?? null,
    tagIds: Array.isArray(d.tagIds) ? (d.tagIds as string[]) : [],
    createdAt: (d.createdAt as number) ?? 0,
    isDeleted: (d.isDeleted as boolean) ?? false,
  }
}

export function mapFood(id: string, d: Record<string, unknown>): FoodEntry {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    occurredAt: (d.occurredAt as number) ?? 0,
    items: (d.items as string) ?? '',
    mealTag: (d.mealTag as MealTag) ?? null,
    createdAt: (d.createdAt as number) ?? 0,
    isDeleted: (d.isDeleted as boolean) ?? false,
  }
}

export function mapNote(id: string, d: Record<string, unknown>): NoteEntry {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    occurredAt: (d.occurredAt as number) ?? 0,
    content: (d.content as string) ?? '',
    caffeine: (d.caffeine as boolean) ?? false,
    alcohol: (d.alcohol as boolean) ?? false,
    tagIds: Array.isArray(d.tagIds) ? (d.tagIds as string[]) : [],
    createdAt: (d.createdAt as number) ?? 0,
    isDeleted: (d.isDeleted as boolean) ?? false,
  }
}

export function mapMedicationEntry(id: string, d: Record<string, unknown>): MedicationEntry {
  return {
    id,
    profileId: (d.profileId as string) ?? 'default',
    medicationId: (d.medicationId as string) ?? '',
    medicationName: (d.medicationName as string) ?? '',
    dose: (d.dose as string) ?? '',
    quantity: (d.quantity as string) ?? '',
    occurredAt: (d.occurredAt as number) ?? 0,
    scheduleId: (d.scheduleId as string) ?? null,
    notes: (d.notes as string) ?? null,
    createdAt: (d.createdAt as number) ?? 0,
    isDeleted: (d.isDeleted as boolean) ?? false,
  }
}

function useCollection<T extends { profileId: string }>(
  userId: string,
  name: string,
  profileId: string,
  mapper: (id: string, d: Record<string, unknown>) => T,
  isDeleted: (t: T) => boolean,
) {
  const [items, setItems] = useState<T[]>([])
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    setLoading(true)
    // No server-side profileId filter: docs written before multi-profile have no
    // profileId field, which an equality filter would silently exclude. Fetch all
    // and filter client-side, where the mapper defaults a missing profileId to 'default'.
    const q = query(collection(db, 'users', userId, name))
    return onSnapshot(q, (snap) => {
      setItems(
        snap.docs
          .map((d) => mapper(d.id, d.data()))
          .filter((t) => !isDeleted(t) && t.profileId === profileId),
      )
      setLoading(false)
    })
  }, [userId, name, profileId])
  return { items, loading }
}

export interface PoopInput {
  occurredAt: number
  bristolTypes: number[]
  blood: number
  notes: string | null
}
export interface FoodInput {
  occurredAt: number
  items: string
  mealTag: MealTag | null
}
export interface NoteInput {
  occurredAt: number
  content: string
  caffeine: boolean
  alcohol: boolean
}
/** A dose, whether logged by hand or edited after a schedule added it. */
export interface MedicineInput {
  occurredAt: number
  medicationId: string
  medicationName: string
  /** Strength snapshot from the catalog entry, e.g. "1g". */
  dose: string
  /** How many units, e.g. "2". */
  quantity: string
  notes: string | null
}

export function useEntries(userId: string, profileId: string) {
  const poop = useCollection(userId, 'poop_entries', profileId, mapPoop, (p) => p.isDeleted)
  const food = useCollection(userId, 'food_entries', profileId, mapFood, (f) => f.isDeleted)
  const note = useCollection(userId, 'note_entries', profileId, mapNote, (n) => n.isDeleted)
  const medicine = useCollection(userId, 'medication_entries', profileId, mapMedicationEntry, (m) => m.isDeleted)

  const loading = poop.loading || food.loading || note.loading || medicine.loading

  const timeline = useMemo<TimelineEntry[]>(() => {
    const all: TimelineEntry[] = [
      ...poop.items.map((entry) => ({ kind: 'poop' as const, occurredAt: entry.occurredAt, entry })),
      ...food.items.map((entry) => ({ kind: 'food' as const, occurredAt: entry.occurredAt, entry })),
      ...note.items.map((entry) => ({ kind: 'note' as const, occurredAt: entry.occurredAt, entry })),
      ...medicine.items.map((entry) => ({ kind: 'medicine' as const, occurredAt: entry.occurredAt, entry })),
    ]
    return all.sort((a, b) => b.occurredAt - a.occurredAt)
  }, [poop.items, food.items, note.items, medicine.items])

  const col = (name: string) => collection(db, 'users', userId, name)

  const addPoop = async (input: PoopInput) => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('poop_entries'), id), {
      userId, profileId,
      occurredAt: input.occurredAt,
      bristolTypes: [...input.bristolTypes].sort((a, b) => a - b),
      blood: input.blood,
      notes: input.notes,
      tagIds: [],
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }
  const updatePoop = async (id: string, input: PoopInput) => {
    await updateDoc(doc(col('poop_entries'), id), {
      occurredAt: input.occurredAt,
      bristolTypes: [...input.bristolTypes].sort((a, b) => a - b),
      blood: input.blood,
      notes: input.notes,
      updatedAt: serverTimestamp(),
    })
  }

  const addFood = async (input: FoodInput) => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('food_entries'), id), {
      userId, profileId,
      occurredAt: input.occurredAt,
      items: input.items,
      mealTag: input.mealTag,
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }
  const updateFood = async (id: string, input: FoodInput) => {
    await updateDoc(doc(col('food_entries'), id), {
      occurredAt: input.occurredAt,
      items: input.items,
      mealTag: input.mealTag,
      updatedAt: serverTimestamp(),
    })
  }

  const addNote = async (input: NoteInput) => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('note_entries'), id), {
      userId, profileId,
      occurredAt: input.occurredAt,
      content: input.content,
      caffeine: input.caffeine,
      alcohol: input.alcohol,
      tagIds: [],
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }
  const updateNote = async (id: string, input: NoteInput) => {
    await updateDoc(doc(col('note_entries'), id), {
      occurredAt: input.occurredAt,
      content: input.content,
      caffeine: input.caffeine,
      alcohol: input.alcohol,
      updatedAt: serverTimestamp(),
    })
  }

  const addMedicine = async (input: MedicineInput) => {
    const id = crypto.randomUUID()
    await setDoc(doc(col('medication_entries'), id), {
      userId, profileId,
      medicationId: input.medicationId,
      medicationName: input.medicationName,
      dose: input.dose,
      quantity: input.quantity,
      occurredAt: input.occurredAt,
      scheduleId: null,
      notes: input.notes,
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }
  const updateMedicine = async (id: string, input: MedicineInput) => {
    await updateDoc(doc(col('medication_entries'), id), {
      medicationId: input.medicationId,
      medicationName: input.medicationName,
      dose: input.dose,
      quantity: input.quantity,
      occurredAt: input.occurredAt,
      notes: input.notes,
      updatedAt: serverTimestamp(),
    })
  }

  const softDelete = (name: string) => async (id: string) => {
    await updateDoc(doc(col(name), id), { isDeleted: true, updatedAt: serverTimestamp() })
  }

  return {
    poops: poop.items,
    foods: food.items,
    notes: note.items,
    medicationEntries: medicine.items,
    timeline,
    loading,
    addPoop, updatePoop, deletePoop: softDelete('poop_entries'),
    addFood, updateFood, deleteFood: softDelete('food_entries'),
    addNote, updateNote, deleteNote: softDelete('note_entries'),
    addMedicine, updateMedicine, deleteMedicine: softDelete('medication_entries'),
  }
}
