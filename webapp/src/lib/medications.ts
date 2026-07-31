// Medication helpers — the TypeScript mirror of
// app/.../data/model/Medication.kt. Both surfaces must agree on which days a
// schedule fires and on the derived id of a materialised dose, or the two apps
// would produce different (or duplicate) doses for the same schedule.

import { DoseStatus, MedicationForm, RepeatRule } from '../types'

export const MEDICATION_FORMS: { value: MedicationForm; label: string }[] = [
  { value: 'TABLET', label: 'Tablet' },
  { value: 'CAPSULE', label: 'Capsule' },
  { value: 'GRANULES', label: 'Granules' },
  { value: 'LIQUID', label: 'Liquid' },
  { value: 'FOAM', label: 'Foam' },
  { value: 'INJECTION', label: 'Injection' },
  { value: 'OTHER', label: 'Other' },
]

export const formLabel = (form: MedicationForm): string =>
  MEDICATION_FORMS.find((f) => f.value === form)?.label ?? 'Tablet'

/** Rectal/topical forms read differently from something you swallow. */
export const formEmoji = (form: MedicationForm): string => (form === 'FOAM' ? '🧴' : '💊')

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

export const DOSE_STATUS_LABELS: Record<DoseStatus, string> = {
  SCHEDULED: 'As scheduled',
  TAKEN: 'Taken',
  SKIPPED: 'Skipped',
  ADJUSTED: 'Adjusted',
  MANUAL: 'Logged',
}

/** CSS colour token for a dose state; the auto-recorded default stays deliberately quiet. */
export function doseStatusColor(status: DoseStatus): string {
  switch (status) {
    case 'TAKEN':
    case 'MANUAL':
      return 'var(--success-text)'
    case 'ADJUSTED':
      return 'var(--warning)'
    case 'SKIPPED':
      return 'var(--danger-text)'
    default:
      return 'var(--fg-faint)'
  }
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

/** "2 g", or just the amount when no unit is set. */
export const formatDose = (amount: string, unit: string): string =>
  [amount.trim(), unit.trim()].filter(Boolean).join(' ')

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
