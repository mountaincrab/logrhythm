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

export type EntryKind = 'poop' | 'food' | 'note'

export type TimelineEntry =
  | { kind: 'poop'; occurredAt: number; entry: PoopEntry }
  | { kind: 'food'; occurredAt: number; entry: FoodEntry }
  | { kind: 'note'; occurredAt: number; entry: NoteEntry }
