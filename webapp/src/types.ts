// Domain types mirroring the Firestore documents written by the Android app.
// Collections live under users/{uid}/<collection>. See
// app/.../data/remote/FirestoreRepository.kt for the source-of-truth shapes.

export type MealTag = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | 'DRINK'

export type AppThemeName = 'DEEP_NAVY' | 'CHARCOAL' | 'RETRO'

export interface Profile {
  id: string
  name: string
  theme: AppThemeName
  createdAt: number
  updatedAt: number
  isDeleted: boolean
}

export interface PoopEntry {
  id: string
  profileId: string
  occurredAt: number
  /** Bristol stool types selected (1..7). */
  bristolTypes: number[]
  /** 1..5 blood rating. 1 = none, 5 = loads. */
  blood: number
  notes: string | null
  tagIds: string[]
  createdAt: number
  isDeleted: boolean
}

export interface FoodEntry {
  id: string
  profileId: string
  occurredAt: number
  items: string
  mealTag: MealTag | null
  createdAt: number
  isDeleted: boolean
}

export interface NoteEntry {
  id: string
  profileId: string
  occurredAt: number
  content: string
  caffeine: boolean
  alcohol: boolean
  tagIds: string[]
  createdAt: number
  isDeleted: boolean
}

export interface Tag {
  id: string
  name: string
  sortOrder: number
  isDeleted: boolean
}

export type MedicationForm =
  | 'TABLET' | 'GRANULES' | 'FOAM' | 'ENEMA' | 'SUPPOSITORY'

export type RepeatRule = 'DAILY' | 'EVERY_OTHER_DAY' | 'WEEKDAYS' | 'SPECIFIC_DAYS'

/**
 * A medication, defined once and referenced by both scheduled and recorded doses:
 * name + form + strength, e.g. "Pentasa, tablet, 1g". How many you take is a dose's
 * quantity, not part of the definition.
 */
export interface Medication {
  id: string
  profileId: string
  name: string
  form: MedicationForm
  /** Strength of a single unit, e.g. "1g". */
  dose: string
  sortOrder: number
  createdAt: number
  isDeleted: boolean
}

/** One scheduled dose: a medication, a quantity, a time of day and a repeat rule. */
export interface MedicationSchedule {
  id: string
  profileId: string
  medicationId: string
  /** How many units of the medication, e.g. "2". */
  quantity: string
  /** Minutes since local midnight, e.g. 480 for 08:00. */
  timeMinutes: number
  repeatRule: RepeatRule
  /** ISO days of week (Mon = 1 … Sun = 7). Only meaningful for SPECIFIC_DAYS. */
  daysOfWeek: number[]
  /** Local epoch-day the schedule starts on; also the EVERY_OTHER_DAY parity anchor. */
  startEpochDay: number
  isActive: boolean
  createdAt: number
  isDeleted: boolean
}

/**
 * A recorded dose — a real timeline entry, exactly like a poop / food / note. There is no
 * taken/skipped state: the row existing is the record, so a dose you missed is deleted and
 * one you took differently has its quantity edited.
 */
export interface MedicationEntry {
  id: string
  profileId: string
  medicationId: string
  /** Snapshots, so a dose still reads correctly if its catalog entry is edited or deleted. */
  medicationName: string
  dose: string
  quantity: string
  occurredAt: number
  /** The scheduled dose this materialised from, or null when logged by hand. */
  scheduleId: string | null
  notes: string | null
  createdAt: number
  isDeleted: boolean
}

export type EntryKind = 'poop' | 'food' | 'note' | 'medicine'

export type TimelineEntry =
  | { kind: 'poop'; occurredAt: number; entry: PoopEntry }
  | { kind: 'food'; occurredAt: number; entry: FoodEntry }
  | { kind: 'note'; occurredAt: number; entry: NoteEntry }
  | { kind: 'medicine'; occurredAt: number; entry: MedicationEntry }
