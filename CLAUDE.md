# LogRhythm — Claude guidance

LogRhythm tracks IBD-relevant signal: poop entries (Bristol type + blood rating + notes), food, and free-form notes. It's modeled after [crab-do](~/git/crab-do) and shares that app's KMP + Compose + Room stack. There are two surfaces: a native **Android** app and a **React web app** (`webapp/`), both backed by the same Firebase project.

## Project status

- **Android app** + **web app** (`webapp/`), both live. iOS is still out of scope.
- **Firebase auth + Firestore sync.** Users sign in (Google); each device syncs through `users/{uid}/…` in Firestore. `userId` is the Firebase uid. `SyncStatus` (PENDING/SYNCED) drives the Android `SyncWorker`; the webapp reads/writes Firestore directly with no local cache.
- **Multi-profile.** A single Firebase account holds one or more local sub-profiles (e.g. tracking more than one person). Every entry/tag row carries a `profileId` (default profile id is `"default"`). The active profile is a per-device preference.
- All v2 screens from `Poop tracker/` designs are implemented on Android: Home, Add poop, Add food, Add note, History (Calendar + Trends), Entry detail, Settings, plus Sign-in and Profiles. The webapp mirrors these.

## Stack

**Android app**
- Kotlin Multiplatform (Android target only)
- Jetpack Compose + Material3
- Room (KMP runtime, schemas exported to `app/schemas/`)
- Koin DI
- DataStore-Preferences (theme + stool-system pref + active profile)
- Navigation-Compose
- Firebase Auth (Google) + Cloud Firestore; WorkManager-driven `SyncWorker`
- kotlinx-serialization (lightweight, kept for future use)

**Web app** (`webapp/`) — see the "Web app" section below
- React 18 + TypeScript + Vite
- Tailwind CSS (semantic CSS-variable theming, mirrors the Android tokens)
- React Router
- Firebase JS SDK (Auth + Firestore), same project as Android

## Build

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Faster compile-only check:
./gradlew :app:compileDebugKotlinAndroid
```

## Source layout

```
app/src/
  commonMain/kotlin/com/mountaincrab/logrhythm/
    data/
      local/AppDatabase.kt, Migrations.kt
      local/dao/{Poop,Food,Note}EntryDao.kt, {Poop,Note}TagDao.kt, ProfileDao.kt
      local/entity/{Poop,Food,Note}EntryEntity.kt, {Poop,Note}TagEntity.kt, {PoopEntry,NoteEntry}TagCrossRef.kt, ProfileEntity.kt
      model/{Bristol,EntryKind,MealTag,StoolSystem,SyncStatus}.kt
    util/Platform.kt           ← expect: currentTimeMillis(), randomUUID()
  androidMain/kotlin/com/mountaincrab/logrhythm/
    LogRhythmApplication.kt    ← Koin startup
    MainActivity.kt
    di/AppModule.kt
    auth/AuthRepository.kt                  ← Firebase Auth wrapper
    data/remote/FirestoreRepository.kt      ← push/pull mappers (Firestore doc shapes)
    data/repository/{EntryRepository,ProfileRepository}.kt
    sync/{SyncWorker,SyncScheduler}.kt      ← WorkManager push/pull on PENDING rows
    preferences/UserPreferencesRepository.kt
    ui/
      theme/{Theme.kt, ThemeViewModel.kt}   ← AppTheme enum, AppPalette, RatingColors
      navigation/AppNavigation.kt           ← all routes
      auth/{SignInViewModel,SignInScreen}.kt
      profiles/{ProfilesViewModel,ProfilesScreen}.kt
      components/{BottomTabBar,SheetHeader,WhenPicker,RatingPill,TimelineEntryRow}.kt
      home/{HomeViewModel,HomeScreen}.kt
      addentry/{AddPoop,AddFood,AddNote}{ViewModel,Screen}.kt
      history/{HistoryViewModel,HistoryScreen}.kt
      detail/{EntryDetailViewModel,EntryDetailScreen}.kt
      settings/{SettingsViewModel,SettingsScreen}.kt
      util/DateUtils.kt
```

The Android Firebase config (`app/google-services.json`) is gitignored — pull it from the Firebase console.

## Data model

Room tables (local, Android):

```
profiles                ← id, name, theme (AppTheme name), createdAt, updatedAt, syncStatus, isDeleted
poop_entries            ← id, userId, profileId, occurredAt, bristolTypes (Set<Int> bitmask), blood (Int 1–5), notes?, createdAt, updatedAt, syncStatus, isDeleted
food_entries            ← id, userId, profileId, occurredAt, items (String), mealTag (MealTag?), createdAt, updatedAt, syncStatus, isDeleted
note_entries            ← id, userId, profileId, occurredAt, content (String), caffeine (Boolean), alcohol (Boolean), createdAt, updatedAt, syncStatus, isDeleted
poop_tags               ← id, profileId, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus
note_tags               ← id, profileId, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus
poop_entry_tag_refs     ← entryId, tagId  (composite PK — many-to-many join)
note_entry_tag_refs     ← entryId, tagId  (composite PK — many-to-many join)
```

Firestore mirror (the cross-device contract — see `FirestoreRepository.kt`): everything lives under
`users/{uid}/{profiles, poop_entries, food_entries, note_entries, poop_tags, note_tags}`. Differences from the
Room shape: `bristolTypes` is stored as a **sorted array of ints** (not a bitmask); poop/note docs carry a
`tagIds` array instead of join rows; `updatedAt` is a Firestore `serverTimestamp()`. Both surfaces must keep
these field names/shapes in sync — the webapp writes the same documents the Android `SyncWorker` pulls.

Repository: `EntryRepository` (Android-only because it uses Android-style Flow combine) writes local rows with
`syncStatus = PENDING`; `SyncScheduler.enqueue()` kicks `SyncWorker`, which pushes pending rows and pulls
remote deltas via `updatedAt`. The webapp skips Room entirely and talks to Firestore through `onSnapshot`.

## Theme

`AppTheme` enum (DEEP_NAVY, CHARCOAL, RETRO) maps to a Material3 `darkColorScheme` plus a custom `AppPalette` (provided via `LocalAppPalette` composition local) for tokens Material3 doesn't cover (`surfaceRaised`, `surfaceHigh`, `border`, `borderSubtle`, `fgMuted`, `fgFaint`, `accentText`, `accentSoft`, `successText`, `dangerText`, `warning`, gradient endpoints).

Rating colours (1..5) live in `Theme.kt:RatingColors` — mirror of `phone.jsx:RATING_COLORS`.

The webapp re-implements the same three themes as CSS variables in `webapp/src/index.css` and exposes them to
Tailwind via `tailwind.config.js`. The active profile's `theme` is applied as a `data-theme` attribute on
`<html>` (`ProfileContext`). Rating colours / Bristol scale / meal tags live in `webapp/src/lib/`.

## Web app

Lives in `webapp/` (React + TS + Vite + Tailwind + Firebase JS SDK). It reads/writes the same Firestore
documents as Android, so it needs the **same Firebase project**.

```bash
cd webapp
cp .env.local.example .env.local   # fill in the Firebase WEB app config (same project as Android)
npm install
npm run dev                        # http://localhost:5173
npm run build                      # tsc + vite build → dist/
```

Layout:

```
webapp/src/
  firebase.ts                 ← initializes app/auth/db from VITE_FIREBASE_* env vars
  types.ts                    ← TS mirror of the Firestore document shapes
  lib/{bristol,ratings,mealTags,dates,theme}.ts
  contexts/{AuthContext,ProfileContext,EntriesContext}.tsx
  hooks/{useProfiles,useEntries}.ts   ← onSnapshot listeners + CRUD (soft-delete via isDeleted)
  components/{AppShell,BottomNav,ProfileMenu,TimelineEntryRow,Sheet,WhenField}.tsx, sheets/Add{Poop,Food,Note}Sheet.tsx
  pages/{Login,Home,History,EntryDetail,Settings}Page.tsx
```

Auth is Google sign-in (`signInWithPopup`). Entries are filtered by the active profile + `isDeleted == false`
client-side (single-field `profileId` query, so no composite Firestore index is required). `crypto.randomUUID()`
generates doc ids; the default profile id is `"default"` (matches Android's `DEFAULT_PROFILE_ID`).

## Room migrations

Same convention as crab-do:

1. Change entity, bump `@Database(version = N)` in `AppDatabase.kt`.
2. `./gradlew :app:compileDebugKotlinAndroid` to emit `app/schemas/<DB>/N.json`.
3. Diff against `N-1.json` to derive SQL.
4. Add `Migration(N-1, N) { ... }` to `ALL_MIGRATIONS` in `data/local/Migrations.kt`.

Without a matching migration the app crashes on upgrade — that's the intended safety net. In `di/AppModule.kt` use **`fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)`** and nothing else; upgrades **must** be migrated. Never use the unconditional `fallbackToDestructiveMigration(...)` — it silently drops every table when an upgrade migration is missing **or throws**, so a buggy migration becomes total data loss instead of a loud crash. (This shipped once: paired with the wrong `migrate()` signature below, it wiped the migration-seeded default profile — see the scenario section.)

### CRITICAL: migrations must use `migrate(SQLiteConnection)`

The DB is built with `.setDriver(BundledSQLiteDriver())` (see `di/AppModule.kt`). With a driver, Room calls `Migration.migrate(connection: SQLiteConnection)` — **not** the old `migrate(db: SupportSQLiteDatabase)`. Each `Migration` must:

- override `migrate(connection: SQLiteConnection)`, and
- use the `androidx.sqlite.execSQL` extension: `connection.execSQL("...")`.

Overriding the `SupportSQLiteDatabase` variant **compiles fine but throws `kotlin.NotImplementedError` on-device** the first time any migration runs (the base class's `migrate(SQLiteConnection)` is a stub that throws). This shipped once already (lost during a branch merge) and crashed the app on every upgrade.

`MigrationTestHelper` runs the framework (`SupportSQLiteOpenHelper`) path, so its schema tests pass **even with the wrong signature** — a false green. The `allMigrations_overrideSQLiteConnectionMigrate()` reflection test in `MigrationTest` exists specifically to catch this; keep it.

### Think through every install scenario, not just the upgrade

A `Migration(N-1, N)` only covers **one** path: an existing user upgrading. When a feature adds a table/column that other code depends on (e.g. a row the new code assumes exists, like the `"default"` profile), walk through each scenario explicitly before shipping:

- **Upgrading user** (old DB → version N): the migration runs. Backfill/seed any rows the new code expects, and stamp `syncStatus = PENDING` so seeded rows eventually sync.
- **Fresh install** (DB created directly at version N): **migrations do NOT run.** Anything a migration seeds will be absent. Seed it at startup or have the code self-heal — never assume a migration-seeded row exists.
- **Destructive-fallback / downgrade**: tables are dropped and recreated empty; migration-seeded rows are gone.
- **Firestore-synced data**: pull only restores what was actually *written remotely*. A row seeded purely locally by a migration is invisible to other devices until its SyncWorker pushes it — and if that device is wiped (destructive fallback) before the push, the row is lost **everywhere**, while entries referencing it come back orphaned (their `profileId` falls back to a profile that exists nowhere).

Concrete failure this caused: the `8→9` profiles migration created a local `"default"` profile owning all existing data. Fresh installs never got it; an earlier destructive-fallback wipe destroyed it before sync pushed it, so pulled entries pointed at a non-existent profile (the "?" avatar / empty profile list). Fix: `ProfileRepository.ensureDefaultProfile()` self-heals on startup (covers fresh install + orphan recovery) and the destructive fallback is now downgrade-only. The lesson: **seeding inside a migration is necessary but never sufficient** — pair it with a startup invariant check that holds for fresh installs and post-wipe states too.

## Designs

Source-of-truth design mockups live under `Poop tracker/` at the repo root (JSX prototype, plus reference d1/d2/d3 alternates). The implemented design is the **v2 (refined)** variant — see `Poop tracker/v2-screens.jsx` and `concepts.html`. d1 and d2 are kept only for reference.
