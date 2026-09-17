// Medication helpers — the TypeScript mirror of
// app/.../data/model/Medication.kt. Both surfaces must agree on which days a
// schedule fires and on the derived id of a materialised dose, or the two apps
// would produce different (or duplicate) doses for the same schedule.

import { Medication, MedicationEntry, MedicationForm, RepeatRule } from '../types'

export const MEDICATION_FORMS: { value: MedicationForm; label: string }[] = [
  { value: 'TABLET', label: 'Tablet' },
  { value: 'GRANULES', label: 'Granules' },
  { value: 'FOAM', label: 'Foam' },
  { value: 'ENEMA', label: 'Enema' },
  { value: 'SUPPOSITORY', label: 'Suppository' },
]

export const formLabel = (form: MedicationForm): string =>
  MEDICATION_FORMS.find((f) => f.value === form)?.label ?? 'Tablet'

export const REPEAT_RULES: { value: RepeatRule; label: string }[] = [
  { value: 'DAILY', label: 'Every day' },
  { value: 'EVERY_OTHER_DAY', label: 'Every other day' },
  { value: 'WEEKDAYS', label: 'Weekdays' },
  { value: 'SPECIFIC_DAYS', label: 'Specific days' },
]

export const repeatRuleLabel = (rule: RepeatRule): string =>
  REPEAT_RULES.find((r) => r.value === rule)?.label ?? 'Every day'

export const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

/** Human summary of a repeat rule, e.g. "Mon · Thu" for specific days. */
export function describeRepeat(rule: RepeatRule, daysOfWeek: number[]): string {
  if (rule !== 'SPECIFIC_DAYS') return repeatRuleLabel(rule)
  if (daysOfWeek.length === 0) return 'No days picked'
  return [...daysOfWeek].sort((a, b) => a - b).map((d) => DAY_NAMES[d - 1]).join(' · ')
}

export type TimeOfDay = 'MORNING' | 'MIDDAY' | 'EVENING' | 'NIGHT'

export const TIMES_OF_DAY: { value: TimeOfDay; label: string; minutes: number }[] = [
  { value: 'MORNING', label: 'Morning', minutes: 8 * 60 },
  { value: 'MIDDAY', label: 'Midday', minutes: 13 * 60 },
  { value: 'EVENING', label: 'Evening', minutes: 18 * 60 },
  { value: 'NIGHT', label: 'Night', minutes: 21 * 60 },
]

export function timeOfDayFor(minutes: number): TimeOfDay {
  // The small hours belong to the night before, not to the morning.
  if (minutes < 4 * 60) return 'NIGHT'
  if (minutes < 11 * 60) return 'MORNING'
  if (minutes < 16 * 60) return 'MIDDAY'
  if (minutes < 20 * 60) return 'EVENING'
  return 'NIGHT'
}

export const timeOfDayLabel = (minutes: number): string =>
  TIMES_OF_DAY.find((t) => t.value === timeOfDayFor(minutes))?.label ?? ''

/** "08:00" for minutes-since-midnight. */
export function formatMinutesOfDay(minutes: number): string {
  const h = Math.min(23, Math.max(0, Math.floor(minutes / 60)))
  const m = Math.min(59, Math.max(0, minutes % 60))
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/**
 * The strength of one unit as a single string, e.g. "1g" from amount "1" + unit "g".
 *
 * Amount and unit are captured separately when defining a medication, but everything that
 * *shows* a strength wants the one string, so they're joined here and nowhere else. Both
 * sides are free text and either may be blank.
 */
export function formatDose(amount: string, unit: string): string {
  return amount.trim() + unit.trim()
}

/** A medication's strength as one string — the display form of doseAmount + doseUnit. */
export const medicationDose = (m: { doseAmount: string; doseUnit: string }): string =>
  formatDose(m.doseAmount, m.doseUnit)

/**
 * How much was taken, e.g. "2 × 1g" for two 1g tablets. `dose` is the medication's own
 * strength (defined once on the catalog entry) and `quantity` is how many of them.
 */
export function formatDoseAmount(quantity: string, dose: string): string {
  const q = quantity.trim()
  const d = dose.trim()
  if (q && d) return `${q} × ${d}`
  return q || d
}

/**
 * Local epoch-day, matching Java's `LocalDate.toEpochDay()`.
 * Built from the local Y/M/D via Date.UTC so the value doesn't shift with the timezone.
 */
export function localEpochDay(date: Date): number {
  return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86_400_000)
}

/** ISO day-of-week, Mon = 1 … Sun = 7 (JS `getDay()` puts Sunday at 0). */
export const isoDayOfWeek = (date: Date): number => date.getDay() === 0 ? 7 : date.getDay()

/** Local "YYYY-MM-DD" — the date half of a materialised dose's id. */
export function isoDateString(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Wall-clock time on a given local date, DST-safe. */
export function doseMillis(date: Date, timeMinutes: number): number {
  return new Date(
    date.getFullYear(), date.getMonth(), date.getDate(),
    Math.floor(timeMinutes / 60), timeMinutes % 60, 0, 0,
  ).getTime()
}

/**
 * Deterministic id for a materialised dose — the same value the Android app derives, so a
 * dose written on both devices converges to one document instead of duplicating.
 */
export const materialisedDoseId = (scheduleId: string, isoDate: string): string =>
  `${scheduleId}_${isoDate}`

/**
 * Whether a schedule fires on the given local date. `startEpochDay` is both the "not before"
 * bound and the anchor that gives EVERY_OTHER_DAY a stable parity across devices.
 */
export function scheduleOccursOn(
  rule: RepeatRule,
  daysOfWeek: number[],
  startEpochDay: number,
  date: Date,
): boolean {
  const epochDay = localEpochDay(date)
  if (epochDay < startEpochDay) return false
  switch (rule) {
    case 'DAILY':
      return true
    case 'EVERY_OTHER_DAY':
      return (epochDay - startEpochDay) % 2 === 0
    case 'WEEKDAYS':
      return isoDayOfWeek(date) <= 5
    case 'SPECIFIC_DAYS':
      return daysOfWeek.includes(isoDayOfWeek(date))
    default:
      return false
  }
}

/**
 * The numeric value in a free-text amount, or null when there isn't one.
 *
 * A medication's strength (`doseAmount`) and a dose's quantity are both free text, so
 * anything that adds doses up has to agree on what counts as a number. "1", " 0.5 " and
 * "1,5" parse; "1 puff" and "" do not. Mirror of `parseAmount` in Medication.kt.
 */
export function parseAmount(text: string): number | null {
  const t = text.trim().replace(',', '.')
  if (t === '') return null
  // Number('') is 0 and Number(' 1 ') is 1, so the blank check above has to come first.
  const n = Number(t)
  return Number.isFinite(n) && n >= 0 ? n : null
}

/**
 * How much one dose is worth: quantity × the strength of a single unit.
 *
 * A blank or non-numeric quantity counts as one unit. `unitAmount` is null for a medication
 * whose strength isn't numeric ("1 puff"), and the dose is then worth its quantity in bare
 * units — the honest fallback, since there is no number to multiply.
 */
export function doseUnits(quantity: string, unitAmount: number | null): number {
  return (parseAmount(quantity) ?? 1) * (unitAmount ?? 1)
}

/**
 * A total as short text: "27", "3.9", "0.5" — no trailing zeros.
 * `decimals` caps the fraction (2 for totals, 1 for averages).
 */
export function formatMedicationValue(value: number, decimals = 2): string {
  return String(Number(value.toFixed(Math.min(Math.max(decimals, 0), 6))))
}

/**
 * Per-medication series colours for the Trends medication rows — mirrors
 * MedicationSeriesColors in ui/theme/Theme.kt.
 *
 * Four hues, checked for lightness, chroma and colour-blind separation against the card
 * surface. A medication's slot is its position in the catalog, so the colour is stable
 * across ranges; past four the palette repeats, which is safe here because every row is
 * labelled with its medication's name — the colour is a marker, never the identity.
 */
export const MEDICATION_SERIES_COLORS = ['#12A0C4', '#8B5CF6', '#E14D96', '#C08410']

export const medicationSeriesColor = (index: number): string =>
  MEDICATION_SERIES_COLORS[((index % MEDICATION_SERIES_COLORS.length) + MEDICATION_SERIES_COLORS.length) % MEDICATION_SERIES_COLORS.length]

/** What a row falls back to when the catalog row a dose points at has vanished entirely. */
export const UNRESOLVED_MEDICATION = 'Medication'

/** One dose in a merged row: which entry recorded it, when, and anything typed on it. */
export interface MedicationOccurrence {
  entryId: string
  occurredAt: number
  notes: string | null
}

/**
 * A day's doses of one medication, folded into a single row: the medication once, how much
 * of it in total, and the time of each dose.
 *
 * Two Pentasa doses are two entries — deleting or editing one still happens on that entry,
 * which is why `occurrences` keeps the entry ids rather than just the times.
 */
export interface MergedMedicationRow {
  key: string
  name: string
  /** Null when the definition can't be resolved — there's no form to draw then. */
  form: MedicationForm | null
  /** How much the row adds up to, e.g. "4 × 1g" — blank for a medication with no strength. */
  amountText: string
  occurrences: MedicationOccurrence[]
}

/**
 * Fold a day's doses into one row per medication, for the grouped home timeline.
 *
 * Doses merge on their `medicationId` — the catalog row is a live reference, so two doses are
 * the same medication however it has been renamed since, and a dose never carries a drug name
 * of its own to merge on instead.
 *
 * Quantities add up the way `doseUnits` already counts them: a blank or non-numeric quantity
 * is one unit, because there is no number to add. A row standing for a single dose keeps that
 * dose's quantity exactly as typed, so merging never rewrites what one dose said.
 *
 * Rows come back newest-dosed first, matching the feed around them, while the times inside a
 * row run forwards — the row is that medication's day, read left to right.
 * Mirror of `mergeMedicationEntries` in data/model/MergedMedication.kt.
 */
export function mergeMedicationEntries(
  doses: MedicationEntry[],
  medicationsById: Map<string, Medication>,
): MergedMedicationRow[] {
  interface Accumulator {
    key: string
    name: string
    form: MedicationForm | null
    dose: string
    totalQuantity: number
    quantities: string[]
    occurrences: MedicationOccurrence[]
  }
  const accumulators = new Map<string, Accumulator>()

  for (const entry of doses) {
    const key = `med:${entry.medicationId}`
    let accumulator = accumulators.get(key)
    if (!accumulator) {
      const medication = medicationsById.get(entry.medicationId)
      accumulator = {
        key,
        name: medication?.name ?? UNRESOLVED_MEDICATION,
        form: medication?.form ?? null,
        dose: medication ? medicationDose(medication) : '',
        totalQuantity: 0,
        quantities: [],
        occurrences: [],
      }
      accumulators.set(key, accumulator)
    }
    accumulator.totalQuantity += parseAmount(entry.quantity) ?? 1
    const quantity = entry.quantity.trim()
    if (quantity) accumulator.quantities.push(quantity)
    accumulator.occurrences.push({
      entryId: entry.id,
      occurredAt: entry.occurredAt,
      notes: entry.notes?.trim() ? entry.notes : null,
    })
  }

  return [...accumulators.values()]
    .map((accumulator): MergedMedicationRow => {
      const quantityText = accumulator.quantities.length === 0
        ? ''
        : accumulator.occurrences.length === 1
          ? accumulator.quantities[0]
          : formatMedicationValue(accumulator.totalQuantity)
      return {
        key: accumulator.key,
        name: accumulator.name,
        form: accumulator.form,
        amountText: formatDoseAmount(quantityText, accumulator.dose),
        occurrences: [...accumulator.occurrences].sort((a, b) => a.occurredAt - b.occurredAt),
      }
    })
    .sort((a, b) => {
      const latest = (row: MergedMedicationRow) => Math.max(...row.occurrences.map((o) => o.occurredAt))
      return latest(b) - latest(a) || a.name.localeCompare(b.name)
    })
}
