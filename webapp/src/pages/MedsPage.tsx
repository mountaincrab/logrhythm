import { useMemo, useState } from 'react'
import { Pencil, Trash2 } from 'lucide-react'
import AppShell from '../components/AppShell'
import { Field } from '../components/Sheet'
import {
  DayPicker, MedicationDialog, MedicationPicker, RepeatChips, TimeOfDayPicker, inputClass,
} from '../components/MedicationFields'
import { useMedicationsContext } from '../contexts/MedicationsContext'
import { Medication, MedicationSchedule, RepeatRule } from '../types'
import {
  describeRepeat, formEmoji, formLabel, formatDoseAmount, formatMinutesOfDay, medicationDose,
  timeOfDayLabel,
} from '../lib/medications'

type Tab = 'schedule' | 'catalog'

const TABS: { id: Tab; label: string }[] = [
  { id: 'schedule', label: 'Schedule' },
  { id: 'catalog', label: 'Medications' },
]

/**
 * Meds is where medication is *set up* — the medications you take, and the schedules that
 * add dose entries for you. Doses themselves live on the timeline with every other entry;
 * there is deliberately no second place to review or confirm them.
 */
export default function MedsPage() {
  const [tab, setTab] = useState<Tab>('schedule')

  return (
    <AppShell title="Meds" subtitle="Doses are added to your timeline automatically" showProfileSwitcher>
      <div className="flex gap-1.5 mb-5">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={
              'flex-1 md:flex-none px-4 py-2 rounded-full border text-[13px] transition-colors ' +
              (t.id === tab
                ? 'bg-accent-soft border-accent text-accent-text font-semibold'
                : 'bg-surface-raised border-DEFAULT text-fg-muted hover:text-fg')
            }
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'schedule' ? <ScheduleTab /> : <CatalogTab />}
    </AppShell>
  )
}

function EmptyCard({ title, body }: { title: string; body: string }) {
  return (
    <div className="bg-surface-raised border border-DEFAULT rounded-2xl px-4 py-5">
      <p className="font-bold text-fg">{title}</p>
      <p className="text-[13px] text-fg-muted mt-1 leading-relaxed">{body}</p>
    </div>
  )
}

function ActionButton({
  label, onClick, primary = false, danger = false,
}: { label: string; onClick: () => void; primary?: boolean; danger?: boolean }) {
  return (
    <button
      onClick={onClick}
      className={
        'flex-1 py-2 rounded-xl text-xs font-bold transition-colors ' +
        (primary ? 'bg-accent text-accent-fg' : danger ? 'bg-surface-high text-danger-text' : 'bg-surface-high text-fg')
      }
    >
      {label}
    </button>
  )
}

// ── Schedule ───────────────────────────────────────────────────────────────

function ScheduleTab() {
  const { schedules, medications, deleteSchedule, setScheduleActive } = useMedicationsContext()
  const [grouped, setGrouped] = useState(false)
  const [editing, setEditing] = useState<MedicationSchedule | null>(null)
  const [adding, setAdding] = useState(false)

  const byId = useMemo(() => new Map(medications.map((m) => [m.id, m])), [medications])
  const nameOf = (s: MedicationSchedule) => byId.get(s.medicationId)?.name ?? 'Unknown medication'
  const doseOf = (s: MedicationSchedule) => {
    const med = byId.get(s.medicationId)
    return med ? medicationDose(med) : ''
  }

  const groups = useMemo(() => {
    const map = new Map<string, MedicationSchedule[]>()
    for (const s of schedules) map.set(s.medicationId, [...(map.get(s.medicationId) ?? []), s])
    return [...map.values()].sort((a, b) => nameOf(a[0]).localeCompare(nameOf(b[0])))
  }, [schedules, byId])

  const card = (s: MedicationSchedule, showName: boolean) => (
    <ScheduleCard
      key={s.id}
      schedule={s}
      name={nameOf(s)}
      amount={formatDoseAmount(s.quantity, doseOf(s))}
      showName={showName}
      onEdit={() => setEditing(s)}
      onDelete={() => deleteSchedule(s.id)}
      onToggleActive={() => setScheduleActive(s.id, !s.isActive)}
    />
  )

  return (
    <div className="max-w-2xl">
      <div className="flex items-center gap-3 mb-3">
        <span className="flex-1 text-xs font-semibold text-fg-muted">
          {schedules.length === 0
            ? 'No scheduled doses'
            : `${schedules.length} scheduled dose${schedules.length === 1 ? '' : 's'}`}
        </span>
        {/* Same rows either way — a scheduled dose carries its medicationId, so this is
            purely how the schedule reads. */}
        <button
          onClick={() => setGrouped(!grouped)}
          className={
            'px-3 py-1.5 rounded-full border text-[12px] font-semibold transition-colors ' +
            (grouped ? 'bg-accent-soft border-accent text-accent-text' : 'bg-surface-raised border-DEFAULT text-fg-muted')
          }
        >
          {grouped ? 'By medication' : 'By dose'}
        </button>
      </div>

      {schedules.length === 0 ? (
        <EmptyCard
          title="Nothing scheduled"
          body={medications.length > 0
            ? "Add a dose and it'll be logged for you each day. Miss one? Delete it from your timeline."
            : 'Add a medication first, then schedule the doses you take.'}
        />
      ) : grouped ? (
        <div className="space-y-5">
          {groups.map((group) => (
            <div key={group[0].medicationId}>
              <div className="ds-eyebrow mb-2">{nameOf(group[0])}</div>
              <div className="space-y-2">{group.map((s) => card(s, false))}</div>
            </div>
          ))}
        </div>
      ) : (
        <div className="space-y-2">{schedules.map((s) => card(s, true))}</div>
      )}

      {medications.length > 0 && (
        <button
          onClick={() => setAdding(true)}
          className="w-full mt-3 py-3 rounded-xl border border-dashed border-strong text-accent-text text-[13px] font-bold"
        >
          + Add a dose
        </button>
      )}

      {(adding || editing) && (
        <ScheduleDialog
          initial={editing ?? undefined}
          onClose={() => { setAdding(false); setEditing(null) }}
        />
      )}
    </div>
  )
}

function ScheduleCard({
  schedule, name, amount, showName, onEdit, onDelete, onToggleActive,
}: {
  schedule: MedicationSchedule
  name: string
  amount: string
  showName: boolean
  onEdit: () => void
  onDelete: () => void
  onToggleActive: () => void
}) {
  const detail = [amount, describeRepeat(schedule.repeatRule, schedule.daysOfWeek)]
    .filter(Boolean).join(' · ')
  return (
    <div className="bg-surface-raised border border-DEFAULT rounded-2xl px-3.5 py-3">
      <div className="flex items-start gap-3">
        <div className="flex-1 min-w-0">
          {showName && <div className="font-bold text-fg truncate">{name}</div>}
          <div className="text-xs text-fg-muted">{detail}</div>
        </div>
        <div className="text-right shrink-0">
          <div className="font-bold tabular-nums">{formatMinutesOfDay(schedule.timeMinutes)}</div>
          <div className="text-[11px] text-fg-faint">{timeOfDayLabel(schedule.timeMinutes)}</div>
        </div>
      </div>
      <div className="flex gap-1.5 mt-3">
        <ActionButton label={schedule.isActive ? 'Pause' : 'Resume'} onClick={onToggleActive} />
        <ActionButton label="Edit" onClick={onEdit} />
        <ActionButton label="Delete" danger onClick={onDelete} />
      </div>
      {!schedule.isActive && (
        <p className="text-[11px] font-semibold mt-2" style={{ color: 'var(--warning)' }}>
          Paused — not adding doses
        </p>
      )}
    </div>
  )
}

function ScheduleDialog({ initial, onClose }: { initial?: MedicationSchedule; onClose: () => void }) {
  const { medications, addSchedule, updateSchedule, addMedication } = useMedicationsContext()
  const first = medications[0]
  const [medicationId, setMedicationId] = useState<string | null>(initial?.medicationId ?? first?.id ?? null)
  const [quantity, setQuantity] = useState(initial?.quantity ?? '1')
  const [minutes, setMinutes] = useState(initial?.timeMinutes ?? 8 * 60)
  const [rule, setRule] = useState<RepeatRule>(initial?.repeatRule ?? 'DAILY')
  const [days, setDays] = useState<number[]>(initial?.daysOfWeek ?? [])
  const [saving, setSaving] = useState(false)
  const [creating, setCreating] = useState(false)

  const selected = medications.find((m) => m.id === medicationId)
  const canSave = medicationId !== null && (rule !== 'SPECIFIC_DAYS' || days.length > 0)

  const save = async () => {
    if (!medicationId) return
    setSaving(true)
    try {
      const input = { medicationId, quantity, timeMinutes: minutes, repeatRule: rule, daysOfWeek: days }
      if (initial) await updateSchedule(initial.id, input)
      else await addSchedule(input)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <div className="fixed inset-0 z-40 flex items-end sm:items-center justify-center bg-black/60">
        <div className="w-full sm:max-w-md bg-bg sm:rounded-3xl rounded-t-3xl shadow-dialog flex flex-col max-h-[92dvh] overflow-hidden">
          <div className="px-5 pt-4 pb-2 shrink-0">
            <h1 className="text-[22px] font-extrabold tracking-tightish">
              {initial ? 'Edit dose' : 'Add a dose'}
            </h1>
          </div>
          <div className="flex-1 min-h-0 overflow-y-auto no-scrollbar px-5 pb-6 pt-1">
            <Field label="Medication">
              <MedicationPicker
                medications={medications}
                selectedId={medicationId}
                onSelect={(m) => setMedicationId(m.id)}
                onCreateNew={() => setCreating(true)}
              />
            </Field>
            {/* How many units — the strength itself is part of the medication's definition. */}
            <Field label="Quantity" hint={selected && medicationDose(selected) ? `× ${medicationDose(selected)}` : undefined}>
              <input
                className={inputClass}
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                placeholder="e.g. 2"
              />
            </Field>
            <Field label="When">
              <TimeOfDayPicker minutes={minutes} onChange={setMinutes} />
            </Field>
            <Field label="Repeats">
              <RepeatChips value={rule} onChange={setRule} />
              {rule === 'SPECIFIC_DAYS' && (
                <div className="mt-2">
                  <DayPicker
                    days={days}
                    onToggle={(iso) => setDays(days.includes(iso) ? days.filter((d) => d !== iso) : [...days, iso])}
                  />
                </div>
              )}
            </Field>
            <p className="text-[12px] text-fg-muted leading-relaxed">
              This adds one scheduled dose. Taking the same medication morning and night?
              Save this, then add a second dose.
            </p>
          </div>
          <div className="px-5 py-3 border-t border-DEFAULT bg-surface flex gap-2 shrink-0">
            <button onClick={onClose} className="flex-1 px-4 py-3.5 rounded-2xl text-[15px] font-bold bg-surface-high text-fg-muted">
              Cancel
            </button>
            <button
              onClick={save}
              disabled={!canSave || saving}
              className="flex-1 px-4 py-3.5 rounded-2xl text-[15px] font-bold bg-accent text-accent-fg disabled:opacity-40 transition-opacity"
            >
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      </div>

      {creating && (
        <MedicationDialog
          onSave={async (draft) => {
            setMedicationId(await addMedication(draft))
          }}
          onClose={() => setCreating(false)}
        />
      )}
    </>
  )
}

// ── Catalog ────────────────────────────────────────────────────────────────

function CatalogTab() {
  const { medications, addMedication, updateMedication, deleteMedication } = useMedicationsContext()
  const [editing, setEditing] = useState<Medication | null>(null)
  const [adding, setAdding] = useState(false)
  const [confirming, setConfirming] = useState<Medication | null>(null)

  return (
    <div className="max-w-2xl">
      {medications.length === 0 ? (
        <EmptyCard
          title="No medications yet"
          body='Define each medication once here — name, form and strength, e.g. "Pentasa, tablet, 1g" — then pick it when scheduling a dose or logging one by hand.'
        />
      ) : (
        <div className="space-y-2">
          {medications.map((m) => {
            return (
              <div key={m.id} className="bg-surface-raised border border-DEFAULT rounded-2xl px-3.5 py-3 flex items-center gap-3">
                <span className="text-xl shrink-0">{formEmoji(m.form)}</span>
                <div className="flex-1 min-w-0">
                  <div className="font-bold text-fg truncate">{m.name}</div>
                  <div className="text-xs text-fg-muted">{[formLabel(m.form), medicationDose(m)].filter(Boolean).join(' · ')}</div>
                </div>
                <button onClick={() => setEditing(m)} className="p-2 text-fg-muted hover:text-fg transition-colors" aria-label="Edit">
                  <Pencil size={16} />
                </button>
                <button onClick={() => setConfirming(m)} className="p-2 text-danger-text hover:opacity-80 transition-opacity" aria-label="Delete">
                  <Trash2 size={16} />
                </button>
              </div>
            )
          })}
        </div>
      )}

      <button
        onClick={() => setAdding(true)}
        className="w-full mt-3 py-3 rounded-xl border border-dashed border-strong text-accent-text text-[13px] font-bold"
      >
        + Add medication
      </button>

      {(adding || editing) && (
        <MedicationDialog
          initial={editing ?? undefined}
          onSave={async (draft) => {
            if (editing) await updateMedication(editing.id, draft)
            else await addMedication(draft)
          }}
          onClose={() => { setAdding(false); setEditing(null) }}
        />
      )}

      {confirming && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-5">
          <div className="w-full max-w-sm bg-bg rounded-3xl shadow-dialog p-5">
            <h2 className="text-lg font-extrabold">Delete {confirming.name}?</h2>
            <p className="text-[13px] text-fg-muted mt-2 leading-relaxed">
              Its scheduled doses stop, but doses already recorded stay on your timeline.
            </p>
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => setConfirming(null)}
                className="flex-1 px-4 py-3 rounded-2xl text-[15px] font-bold bg-surface-high text-fg-muted"
              >
                Cancel
              </button>
              <button
                onClick={async () => { await deleteMedication(confirming.id); setConfirming(null) }}
                className="flex-1 px-4 py-3 rounded-2xl text-[15px] font-bold bg-surface-high text-danger-text"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
