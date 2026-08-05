import { useState } from 'react'
import { Field } from './Sheet'
import { Medication, MedicationForm, RepeatRule } from '../types'
import {
  DAY_NAMES, MEDICATION_FORMS, REPEAT_RULES, TIMES_OF_DAY,
  formEmoji, formatMinutesOfDay, timeOfDayFor,
} from '../lib/medications'

const chipClass = (on: boolean) =>
  'px-3 py-2 rounded-full border text-[13px] transition-colors ' +
  (on ? 'bg-accent-soft border-accent text-accent-text font-semibold' : 'bg-surface-raised border-DEFAULT text-fg')

export const inputClass =
  'w-full bg-surface-raised border border-DEFAULT rounded-xl px-3.5 py-3 text-[15px] font-semibold text-fg ' +
  'outline-none focus:border-accent placeholder:text-fg-faint placeholder:font-normal transition-colors'

export function Chip({ label, on, onClick }: { label: string; on: boolean; onClick: () => void }) {
  return <button type="button" onClick={onClick} className={chipClass(on)}>{label}</button>
}

export function FormChips({ value, onChange }: { value: MedicationForm; onChange: (f: MedicationForm) => void }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {MEDICATION_FORMS.map((f) => (
        <Chip key={f.value} label={f.label} on={f.value === value} onClick={() => onChange(f.value)} />
      ))}
    </div>
  )
}

export function RepeatChips({ value, onChange }: { value: RepeatRule; onChange: (r: RepeatRule) => void }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {REPEAT_RULES.map((r) => (
        <Chip key={r.value} label={r.label} on={r.value === value} onClick={() => onChange(r.value)} />
      ))}
    </div>
  )
}

/** Mon–Sun toggles, shown only for SPECIFIC_DAYS. */
export function DayPicker({ days, onToggle }: { days: number[]; onToggle: (iso: number) => void }) {
  return (
    <div className="grid grid-cols-7 gap-1.5">
      {DAY_NAMES.map((name, i) => {
        const iso = i + 1
        const on = days.includes(iso)
        return (
          <button
            key={name}
            type="button"
            onClick={() => onToggle(iso)}
            className={
              'py-2.5 rounded-lg border text-[13px] font-bold transition-colors ' +
              (on ? 'bg-accent-soft border-accent text-accent-text' : 'bg-surface-raised border-DEFAULT text-fg-muted')
            }
          >
            {name[0]}
          </button>
        )
      })}
    </div>
  )
}

/** The four time-of-day buckets, each seeding a sensible default time. */
export function TimeOfDayPicker({ minutes, onChange }: { minutes: number; onChange: (m: number) => void }) {
  const active = timeOfDayFor(minutes)
  return (
    <div className="space-y-2">
      <div className="grid grid-cols-4 gap-1.5">
        {TIMES_OF_DAY.map((t) => (
          <button
            key={t.value}
            type="button"
            onClick={() => onChange(t.minutes)}
            className={
              'py-2.5 rounded-xl border text-[12px] font-bold transition-colors ' +
              (t.value === active ? 'bg-accent-soft border-accent text-accent-text' : 'bg-surface-raised border-DEFAULT text-fg-muted')
            }
          >
            {t.label}
          </button>
        ))}
      </div>
      <input
        type="time"
        value={formatMinutesOfDay(minutes)}
        onChange={(e) => {
          const [h, m] = e.target.value.split(':').map(Number)
          if (!Number.isNaN(h) && !Number.isNaN(m)) onChange(h * 60 + m)
        }}
        className={inputClass + ' [color-scheme:dark] tabular-nums'}
      />
    </div>
  )
}

/**
 * Picks a medication out of the catalog. Creating one inline is deliberate — a medication is
 * defined once and then referenced by both manual doses and scheduled ones.
 */
export function MedicationPicker({
  medications, selectedId, onSelect, onCreateNew,
}: {
  medications: Medication[]
  selectedId: string | null
  onSelect: (m: Medication) => void
  onCreateNew: () => void
}) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {medications.map((m) => {
        return (
          <Chip
            key={m.id}
            label={`${formEmoji(m.form)} ${m.name}${m.dose ? ` · ${m.dose}` : ''}`}
            on={m.id === selectedId}
            onClick={() => onSelect(m)}
          />
        )
      })}
      <button
        type="button"
        onClick={onCreateNew}
        className="px-3 py-2 rounded-full border border-subtle bg-surface-raised text-[13px] text-fg-muted hover:text-fg transition-colors"
      >
        + New medication
      </button>
    </div>
  )
}

export interface MedicationDraft {
  name: string
  form: MedicationForm
  dose: string
}

/**
 * Create/edit a catalog medication — name, form and the strength of one unit, e.g.
 * "Pentasa, tablet, 1g". How many you take isn't defined here; that's a dose's quantity.
 */
export function MedicationDialog({
  initial, onSave, onClose,
}: {
  initial?: Medication
  onSave: (draft: MedicationDraft) => Promise<void> | void
  onClose: () => void
}) {
  const [name, setName] = useState(initial?.name ?? '')
  const [form, setForm] = useState<MedicationForm>(initial?.form ?? 'TABLET')
  const [dose, setDose] = useState(initial?.dose ?? '')
  const [saving, setSaving] = useState(false)

  const save = async () => {
    if (!name.trim()) return
    setSaving(true)
    try {
      await onSave({ name, form, dose })
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60">
      <div className="w-full sm:max-w-md bg-bg sm:rounded-3xl rounded-t-3xl shadow-dialog flex flex-col max-h-[92dvh] overflow-hidden">
        <div className="px-5 pt-4 pb-2 shrink-0">
          <h1 className="text-[22px] font-extrabold tracking-tightish">
            {initial ? 'Edit medication' : 'New medication'}
          </h1>
        </div>
        <div className="flex-1 min-h-0 overflow-y-auto no-scrollbar px-5 pb-6 pt-1">
          <Field label="Name">
            <input
              autoFocus
              className={inputClass}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Pentasa"
            />
          </Field>
          <Field label="Form">
            <FormChips value={form} onChange={setForm} />
          </Field>
          <Field label="Dose" hint="strength of one unit">
            <input
              className={inputClass}
              value={dose}
              onChange={(e) => setDose(e.target.value)}
              placeholder="e.g. 1g"
            />
          </Field>
        </div>
        <div className="px-5 py-3 border-t border-DEFAULT bg-surface flex gap-2 shrink-0">
          <button onClick={onClose} className="flex-1 px-4 py-3.5 rounded-2xl text-[15px] font-bold bg-surface-high text-fg-muted">
            Cancel
          </button>
          <button
            onClick={save}
            disabled={!name.trim() || saving}
            className="flex-1 px-4 py-3.5 rounded-2xl text-[15px] font-bold bg-accent text-accent-fg disabled:opacity-40 transition-opacity"
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}
