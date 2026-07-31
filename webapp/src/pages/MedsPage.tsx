import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Pencil, Trash2 } from 'lucide-react'
import AppShell from '../components/AppShell'
import { Field } from '../components/Sheet'
import {
  DayPicker, DoseFields, MedicationDialog, MedicationPicker, RepeatChips, TimeOfDayPicker,
} from '../components/MedicationFields'
import { useMedicationsContext } from '../contexts/MedicationsContext'
import { useEntriesContext } from '../contexts/EntriesContext'
import { Medication, MedicationEntry, MedicationSchedule, RepeatRule } from '../types'
import {
  DOSE_STATUS_LABELS, describeRepeat, doseStatusColor, formEmoji, formLabel,
  formatDose, formatMinutesOfDay, timeOfDayLabel,
} from '../lib/medications'
import { dayKey, formatDayShort, formatTime } from '../lib/dates'
import { UpcomingDose } from '../hooks/useMedications'

type Tab = 'today' | 'schedule' | 'catalog'

const TABS: { id: Tab; label: string }[] = [
  { id: 'today', label: 'Today' },
  { id: 'schedule', label: 'Schedule' },
  { id: 'catalog', label: 'Medications' },
]

export default function MedsPage() {
  const meds = useMedicationsContext()
  const { medicationEntries, setDoseStatus } = useEntriesContext()
  const [tab, setTab] = useState<Tab>('today')

  const todaysDoses = useMemo(() => {
    const today = dayKey(Date.now())
    return medicationEntries
      .filter((e) => dayKey(e.occurredAt) === today)
      .sort((a, b) => a.occurredAt - b.occurredAt)
  }, [medicationEntries])

  const total = todaysDoses.length + meds.upcoming.length
  const subtitle = total === 0
    ? `${formatDayShort(Date.now())} · nothing scheduled`
    : `${formatDayShort(Date.now())} · ${todaysDoses.length} of ${total} doses so far`

  return (
    <AppShell title="Meds" subtitle={subtitle} showProfileSwitcher>
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

      {tab === 'today' && (
        <TodayTab
          recorded={todaysDoses}
          upcoming={meds.upcoming}
          onStatus={setDoseStatus}
          onConfirm={meds.confirmUpcoming}
        />
      )}
      {tab === 'schedule' && <ScheduleTab />}
      {tab === 'catalog' && <CatalogTab />}
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

// ── Today ──────────────────────────────────────────────────────────────────

function TodayTab({
  recorded, upcoming, onStatus, onConfirm,
}: {
  recorded: MedicationEntry[]
  upcoming: UpcomingDose[]
  onStatus: (id: string, status: MedicationEntry['status'], amount?: string, unit?: string) => Promise<void>
  onConfirm: (dose: UpcomingDose, status: 'TAKEN' | 'SKIPPED') => Promise<void>
}) {
  if (recorded.length === 0 && upcoming.length === 0) {
    return (
      <EmptyCard
        title="No doses today"
        body="Add a medication and put it on a schedule — doses then record themselves, and you only step in when something changes."
      />
    )
  }
  return (
    <div className="space-y-2 max-w-2xl">
      {recorded.map((entry) => <RecordedDose key={entry.id} entry={entry} onStatus={onStatus} />)}
      {upcoming.map((dose) => <UpcomingDoseCard key={dose.schedule.id} dose={dose} onConfirm={onConfirm} />)}
    </div>
  )
}

function RecordedDose({
  entry, onStatus,
}: {
  entry: MedicationEntry
  onStatus: (id: string, status: MedicationEntry['status'], amount?: string, unit?: string) => Promise<void>
}) {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [adjusting, setAdjusting] = useState(false)
  const [amount, setAmount] = useState(entry.amount)
  const [unit, setUnit] = useState(entry.unit)
  const dose = formatDose(entry.amount, entry.unit)

  return (
    <div className="bg-surface-raised border border-DEFAULT rounded-2xl px-3.5 py-3">
      <button onClick={() => setOpen(!open)} className="w-full text-left flex items-center gap-3">
        <span className="text-[13px] font-bold tabular-nums text-fg-muted">{formatTime(entry.occurredAt)}</span>
        <span className="flex-1 min-w-0">
          <span className="block font-bold text-fg truncate">{entry.medicationName}</span>
          {dose && <span className="block text-xs text-fg-muted">{dose}</span>}
        </span>
        <span className="text-[11px] font-bold shrink-0" style={{ color: doseStatusColor(entry.status) }}>
          {DOSE_STATUS_LABELS[entry.status]}
        </span>
      </button>

      {open && !adjusting && (
        <div className="mt-3 space-y-2">
          <div className="flex gap-1.5">
            <ActionButton label="Took it" primary onClick={() => onStatus(entry.id, 'TAKEN')} />
            <ActionButton label="Skipped" onClick={() => onStatus(entry.id, 'SKIPPED')} />
            <ActionButton label="Amount" onClick={() => setAdjusting(true)} />
          </div>
          <ActionButton label="Open entry" onClick={() => navigate(`/entry/medicine/${entry.id}`)} />
        </div>
      )}

      {open && adjusting && (
        <div className="mt-3 space-y-2">
          <p className="text-[12px] text-fg-muted">
            Record what you actually took — the dose stays on the timeline, marked as adjusted.
          </p>
          <DoseFields amount={amount} unit={unit} onAmount={setAmount} onUnit={setUnit} />
          <div className="flex gap-1.5">
            <ActionButton label="Cancel" onClick={() => setAdjusting(false)} />
            <ActionButton
              label="Save"
              primary
              onClick={async () => {
                await onStatus(entry.id, 'ADJUSTED', amount.trim(), unit.trim())
                setAdjusting(false)
                setOpen(false)
              }}
            />
          </div>
        </div>
      )}
    </div>
  )
}

function UpcomingDoseCard({
  dose, onConfirm,
}: { dose: UpcomingDose; onConfirm: (d: UpcomingDose, s: 'TAKEN' | 'SKIPPED') => Promise<void> }) {
  const [open, setOpen] = useState(false)
  const amount = formatDose(dose.schedule.amount, dose.schedule.unit)
  return (
    <div className="bg-accent-soft border border-accent rounded-2xl px-3.5 py-3">
      <button onClick={() => setOpen(!open)} className="w-full text-left flex items-center gap-3">
        <span className="text-[13px] font-bold tabular-nums text-accent-text">
          {formatMinutesOfDay(dose.schedule.timeMinutes)}
        </span>
        <span className="flex-1 min-w-0">
          <span className="block font-bold text-fg truncate">{dose.medication.name}</span>
          <span className="block text-xs text-fg-muted">
            {amount ? `${amount} · due later today` : 'Due later today'}
          </span>
        </span>
        <span className="text-[11px] font-bold text-accent-text shrink-0">Due</span>
      </button>
      {open && (
        <div className="mt-3 flex gap-1.5">
          <ActionButton label="Took it now" primary onClick={() => onConfirm(dose, 'TAKEN')} />
          <ActionButton label="Skip" onClick={() => onConfirm(dose, 'SKIPPED')} />
        </div>
      )}
    </div>
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
            ? "Add a dose and it'll record itself each day — you only touch it when you miss one."
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
  schedule, name, showName, onEdit, onDelete, onToggleActive,
}: {
  schedule: MedicationSchedule
  name: string
  showName: boolean
  onEdit: () => void
  onDelete: () => void
  onToggleActive: () => void
}) {
  const dose = formatDose(schedule.amount, schedule.unit)
  const detail = [dose, describeRepeat(schedule.repeatRule, schedule.daysOfWeek)].filter(Boolean).join(' · ')
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
          Paused — not recording doses
        </p>
      )}
    </div>
  )
}

function ScheduleDialog({ initial, onClose }: { initial?: MedicationSchedule; onClose: () => void }) {
  const { medications, addSchedule, updateSchedule, addMedication } = useMedicationsContext()
  const first = medications[0]
  const [medicationId, setMedicationId] = useState<string | null>(initial?.medicationId ?? first?.id ?? null)
  const [amount, setAmount] = useState(initial?.amount ?? first?.defaultAmount ?? '')
  const [unit, setUnit] = useState(initial?.unit ?? first?.defaultUnit ?? '')
  const [minutes, setMinutes] = useState(initial?.timeMinutes ?? 8 * 60)
  const [rule, setRule] = useState<RepeatRule>(initial?.repeatRule ?? 'DAILY')
  const [days, setDays] = useState<number[]>(initial?.daysOfWeek ?? [])
  const [saving, setSaving] = useState(false)
  const [creating, setCreating] = useState(false)

  const canSave = medicationId !== null && (rule !== 'SPECIFIC_DAYS' || days.length > 0)

  const save = async () => {
    if (!medicationId) return
    setSaving(true)
    try {
      const input = { medicationId, amount, unit, timeMinutes: minutes, repeatRule: rule, daysOfWeek: days }
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
                onSelect={(m) => { setMedicationId(m.id); setAmount(m.defaultAmount); setUnit(m.defaultUnit) }}
                onCreateNew={() => setCreating(true)}
              />
            </Field>
            <Field label="Dose" hint="optional">
              <DoseFields amount={amount} unit={unit} onAmount={setAmount} onUnit={setUnit} />
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
            const id = await addMedication(draft)
            setMedicationId(id)
            setAmount(draft.defaultAmount)
            setUnit(draft.defaultUnit)
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
          body="Define each medication once here, then pick it when scheduling a dose or logging one by hand."
        />
      ) : (
        <div className="space-y-2">
          {medications.map((m) => {
            const dose = formatDose(m.defaultAmount, m.defaultUnit)
            return (
              <div key={m.id} className="bg-surface-raised border border-DEFAULT rounded-2xl px-3.5 py-3 flex items-center gap-3">
                <span className="text-xl shrink-0">{formEmoji(m.form)}</span>
                <div className="flex-1 min-w-0">
                  <div className="font-bold text-fg truncate">{m.name}</div>
                  <div className="text-xs text-fg-muted">{[formLabel(m.form), dose].filter(Boolean).join(' · ')}</div>
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
