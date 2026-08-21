# LogRhythm — Claude guidance

LogRhythm tracks IBD-relevant signal: poop entries (Bristol type + blood rating + notes), food, free-form notes, and medication doses. It's modeled after [crab-do](~/git/crab-do) and shares that app's KMP + Compose + Room stack. There are two surfaces: a native **Android** app and a **React web app** (`webapp/`), both backed by the same Firebase project.

## Project status

- **Android app** + **web app** (`webapp/`), both live. iOS is still out of scope.
- **Firebase auth + Firestore sync.** Users sign in (Google); each device syncs through `users/{uid}/…` in Firestore. `userId` is the Firebase uid. `SyncStatus` (PENDING/SYNCED) drives the Android `SyncWorker`; the webapp reads/writes Firestore directly with no local cache.
- **Multi-profile.** A single Firebase account holds one or more local sub-profiles (e.g. tracking more than one person). Every entry/tag row carries a `profileId` (default profile id is `"default"`). The active profile is a per-device preference.
- All v2 screens from `Poop tracker/` designs are implemented on Android: Home, Add poop, Add food, Add note, Add medicine, History (Calendar + Trends), Meds (Schedule / Medications), Entry detail, Settings, plus Sign-in and Profiles. The webapp mirrors these.

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

## CI, versioning & releases

Two GitHub Actions workflows (same setup as crab-do / learning-games):

- **`.github/workflows/android-build.yml`** — runs on **pull requests only** (pushes to
  `main` are handled by the release workflow). Builds the debug APK and uploads it as an
  artifact named `logrhythm-<version>`.
- **`.github/workflows/release.yml`** — runs on **push to `main`**. Job 1 analyses the
  conventional-commit history with `mathieudutour/github-tag-action`, bumps the semver
  tag and pushes it; job 2 (only if a tag was created) builds the tagged commit and
  publishes a GitHub Release with the APK attached and notes generated from the commits.

**Versioning is git-derived** (`app/build.gradle.kts`, mirroring crab-do): `versionCode`
= commit count; `versionName` = latest `vX.Y.Z` tag (clean `X.Y.Z` on `main`/tagged
builds, `X.Y.Z-<branch>.<sha>` on branches). No version-bump commits. CI passes
`VERSION_BRANCH`/`VERSION_SHA` so PR builds version correctly; `./gradlew -q
:app:printVersionName` prints the computed name (used to name the APK).

The **webapp** derives its version the same way, in `webapp/vite.config.ts`: the same
`vX.Y.Z` tag lookup (honouring `VITE_APP_VERSION`/`VERSION_BRANCH`/`VERSION_SHA`), baked
in at build time as the `__APP_VERSION__` global (declared in `webapp/src/vite-env.d.ts`).
Both surfaces show it in **Settings → About** — Android via `BuildConfig.VERSION_NAME`
(`buildConfig = true` in `app/build.gradle.kts`), the webapp via `__APP_VERSION__`. A
build from a clone without tags falls back to `0.0.0`.

**Commit messages must use [Conventional Commits](https://www.conventionalcommits.org/)** —
the release workflow derives both the version bump and the release notes from them:
`feat:` → minor, `fix:`/`ci:`/`chore:`/`docs:`/`refactor:`/`perf:`/`test:`/`style:` →
patch, `feat!:` or a `BREAKING CHANGE:` footer → major. An unprefixed subject is dropped
from the changelog. On squash-merge the **PR title** becomes the `main` commit subject, so
the PR title is what needs the prefix.

**Secrets** (both optional — builds succeed without them): `GOOGLE_SERVICES_JSON`
(base64 of a real `app/google-services.json`; falls back to the committed
`app/google-services.json.placeholder` so Firebase just won't work at runtime) and
`DEBUG_KEYSTORE` (base64 of a debug keystore for a stable signing key / working Google
Sign-In; falls back to an ephemeral key).

## Source layout

```
app/src/
  commonMain/kotlin/com/mountaincrab/logrhythm/
    data/
      local/AppDatabase.kt, Migrations.kt
      local/dao/{Poop,Food,Note}EntryDao.kt, {Poop,Note}TagDao.kt, ProfileDao.kt, Medication{,Schedule,Entry}Dao.kt
      local/entity/{Poop,Food,Note}EntryEntity.kt, {Poop,Note}TagEntity.kt, {PoopEntry,NoteEntry}TagCrossRef.kt, ProfileEntity.kt, Medication{,Schedule,Entry}Entity.kt
      model/{Bristol,EntryKind,MealTag,Medication,StoolSystem,SyncStatus}.kt
    util/Platform.kt           ← expect: currentTimeMillis(), randomUUID()
  androidMain/kotlin/com/mountaincrab/logrhythm/
    LogRhythmApplication.kt    ← Koin startup
    MainActivity.kt
    di/AppModule.kt
    auth/AuthRepository.kt                  ← Firebase Auth wrapper
    data/remote/FirestoreRepository.kt      ← push/pull mappers (Firestore doc shapes)
    data/repository/{EntryRepository,ProfileRepository,MedicationRepository}.kt
    sync/{SyncWorker,SyncScheduler}.kt      ← WorkManager push/pull on PENDING rows
    preferences/UserPreferencesRepository.kt
    ui/
      theme/{Theme.kt, ThemeViewModel.kt}   ← AppTheme enum, AppPalette, RatingColors
      navigation/AppNavigation.kt           ← all routes
      auth/{SignInViewModel,SignInScreen}.kt
      profiles/{ProfilesViewModel,ProfilesScreen}.kt
      components/{BottomTabBar,SheetHeader,WhenPicker,RatingPill,TimelineEntryRow,MedicationIcons}.kt
      home/{HomeViewModel,HomeScreen}.kt
      addentry/{AddPoop,AddFood,AddNote,AddMedicine}{ViewModel,Screen}.kt
      meds/{MedsViewModel,MedsScreen,MedicationComponents}.kt
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
medications             ← id, userId, profileId, name, form (MedicationForm), doseAmount + doseUnit (strength of one unit, e.g. "1" + "g"), sortOrder, createdAt, updatedAt, syncStatus, isArchived
medication_schedules    ← id, userId, profileId, medicationId, quantity, timeMinutes (Int, mins from midnight), repeatRule (RepeatRule), daysMask (ISO day bitmask), startEpochDay, isActive, createdAt, updatedAt, syncStatus, isArchived
medication_entries      ← id, userId, profileId, medicationId, quantity, occurredAt, scheduleId?, notes?, createdAt, updatedAt, syncStatus, isDeleted
```

Firestore mirror (the cross-device contract — see `FirestoreRepository.kt`): everything lives under
`users/{uid}/{profiles, poop_entries, food_entries, note_entries, poop_tags, note_tags, medications,
medication_schedules, medication_entries}`. Differences from the Room shape: `bristolTypes` is stored as a
**sorted array of ints** (not a bitmask); poop/note docs carry a `tagIds` array instead of join rows; a
schedule's `daysMask` bitmask is stored as a sorted `daysOfWeek` **ISO day array** (Mon = 1 … Sun = 7);
`updatedAt` is a Firestore `serverTimestamp()`; `medications` and `medication_schedules` carry `isArchived`
where every other collection carries `isDeleted`. Both surfaces must keep these field names/shapes in sync —
the webapp writes the same documents the Android `SyncWorker` pulls.

## Medication

Medication is a first-class entry type: recorded doses are ordinary timeline rows next to poops, meals and
notes. Three pieces:

- **Catalog** (`medications`) — a medication is defined **once** and referenced everywhere: by scheduled doses
  and by recorded ones. Drug names are never free text on an entry. A medication is *name + form + strength*
  — "Pentasa, tablet, 1g". How many you take is **not** part of the definition. The strength is stored split
  as `doseAmount` + `doseUnit` (both free text) and joined into the one string — `formatDose()` on either
  surface — everywhere it's displayed.
- **Schedule** (`medication_schedules`) — one row per scheduled dose (medication + quantity + time + repeat).
  `quantity` is how many units, so "2" against Pentasa 1g means two 1g tablets. A med taken morning and night
  is two rows. Because each row carries its `medicationId`, the same data renders either as a flat list of
  doses or grouped per medication; the Meds screen ships both behind a toggle, so that choice stays
  presentation-only and never becomes a schema change.
- **Doses** (`medication_entries`) — what happened: which medication, how many units, when. The name and
  strength are **not** copied here; they're read from the catalog through `medicationId` at display time.
  So correcting a definition corrects every dose of it, and a field added to the catalog later (active
  ingredient, prescriber…) shows up on historical doses with no back-fill.

**The schedule exists only to automate adding dose entries.** Once a scheduled dose's time has passed it is
*materialised* into a real timeline row. There is deliberately **no per-dose status** and no review screen:
the row existing is the record. Missed a dose? Delete the entry. Took a different amount? Edit its quantity.
Those are the same two gestures every other entry type uses, and adding a second place to confirm or correct
doses is what made this confusing the first time round. This is not a confirm-every-dose adherence app —
daily tapping is what kills those.

Rules that both surfaces must keep identical (`data/model/Medication.kt` ↔ `webapp/src/lib/medications.ts`):

- A materialised dose's id is **derived**: `{scheduleId}_{yyyy-MM-dd}`. That's what stops the phone and the
  webapp creating two documents for the same dose. Changing this format orphans existing doses.
- `startEpochDay` is both the "not before" bound and the parity anchor for `EVERY_OTHER_DAY`, so every device
  agrees on which days fire. It's a **local** epoch day (`Date.UTC(y, m, d)` on the web, to match Java's
  `LocalDate.toEpochDay()`).
- Only doses whose time has **passed** are written, so the timeline can't claim a dose that hasn't
  happened yet.
- Materialisation runs after the sync pull, is bounded to a 14-day backfill, and skips ids that already exist
  — **including soft-deleted ones**, so a dose the user deleted isn't resurrected on the next pass.
- **Medications and schedules are archived, never deleted** (`isArchived`) — a dose resolves its medication
  through `medicationId`, so the lookup must never miss. Only the pickers and the Meds tabs filter archived
  rows; every lookup path (`observeForLookup` / `medicationsById`) includes them. Entries keep `isDeleted`:
  those genuinely are deletable. Both surfaces have Archived sections that restore.
- A schedule's `isArchived` is distinct from `isActive`: paused stays on the Schedule tab, archived leaves it.
  Neither materialises. **Restoring a schedule resets `startEpochDay` to today** — otherwise the next pass
  back-fills 14 days of doses that never happened. Restoring a medication leaves its schedules archived for
  the same reason.
- Editing a definition is a **correction** and propagates. A prescription change (1g → 2g) is a *different
  medication*: add the new one and archive the old, rather than mutating a row that history points at.
- **Trends totals each medication in its own unit, never across medications.** A dose is worth
  `quantity × parseAmount(doseAmount)`, so 2 × 1g tablets morning and night is 4g that day; a
  strength that isn't numeric ("1 puff") has nothing to multiply by and the row counts units taken
  instead (shown with a `×` suffix). Grams and milligrams share no axis, so each medication is its
  own row scaled to its own peak — there is deliberately no combined total. Rows follow the catalog's
  order, which is also the series-colour slot (`MedicationSeriesColors` ↔ `MEDICATION_SERIES_COLORS`),
  so changing the range never repaints a medication.
- `MedicationForm` is a closed set — `TABLET`, `GRANULES`, `FOAM`, `ENEMA`, `SUPPOSITORY` — and both surfaces
  must list the same values in the same order.
- **Medication icons are drawn, not emoji** (`ui/components/MedicationIcons.kt` ↔
  `webapp/src/components/MedicationIcons.tsx`): one general mark plus one icon per form, each a list of
  filled paths on a 32×32 viewport, and the path data is identical on both surfaces. There is no emoji for
  granules, foam, an enema or a suppository, so the five forms used to share two glyphs — and the busiest of
  them was 💊, which was *also* the app-wide mark for "medicine". The general mark is a bottle precisely
  because it's the one shape in the set that isn't a tablet, so a timeline row can say "a dose" and "which
  form" without the two looking alike. The icons carry their own colours rather than taking a palette tint:
  they sit next to the 💩 / 🍴 / 📝 emoji, so they have to read as part of that family on all three themes.
  Poop, food and notes stay emoji — only medication is drawn. Change a shape on one surface and you must
  change it on the other, or the same dose looks different on phone and web.
- **Entry marks are sized from one place** (`EntryIconSizes` ↔ `ENTRY_ICON_SIZES`), never a literal at the
  call site. The weight to match is the **rating pill** a mark shares its row with, not the 13sp text beside
  it — sized against the text, a dose's icon reads as punctuation next to a poop's rating circle. A drawn
  icon needs a bigger box than the emoji it sits beside: its paths sit inside the 32×32 viewport with a
  couple of units of padding, so only ~78% of the box is ink where an emoji fills its own, and the drawn
  sizes carry that ~1.27× (`drawnIconSize()` on the web). The two surfaces' numbers differ because their
  type scales do (a 24dp rating pill on Android, an 18px circle on the web); the relationships don't.

`MedicationScheduleTest` covers the repeat rules, the derived id and the time-of-day buckets on the Kotlin
side; keep the TS mirror in step with it.

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
  lib/{bristol,ratings,mealTags,medications,dates,theme}.ts
  contexts/{AuthContext,ProfileContext,EntriesContext,MedicationsContext}.tsx
  hooks/{useProfiles,useEntries,useMedications}.ts   ← onSnapshot listeners + CRUD (soft-delete via isDeleted)
  components/{AppShell,Sidebar,MobileNav,ProfileSwitcher,TimelineEntryRow,Sheet,WhenField,MedicationFields,MedicationIcons}.tsx, sheets/Add{Poop,Food,Note,Medicine}Sheet.tsx
  pages/{Login,Home,History,EntryDetail,Meds,Settings}Page.tsx
```

`AppShell` is responsive and switches chrome at Tailwind's `md` (768px) breakpoint — no JS media queries,
just CSS via responsive class prefixes:

- **Desktop (≥ md):** a full-height flex row with a left `Sidebar` (brand, Home/History nav, profile switcher,
  Settings + sign-out) and a content column with a top header bar. Home's log buttons sit in the header (`headerRight`).
- **Phone (< md):** the `Sidebar` is `hidden`; instead a `MobileNav` bottom tab bar (Home/History/Settings) and a
  header `ProfileSwitcher` avatar (tap → bottom-sheet profile picker) mirror the **Android app** layout — the tab
  bar carries Home/History/Meds/Settings on both surfaces. Home's log
  buttons move to a full-width `bottomBar` of vertical emoji cards above the tab bar. `AppShell` exposes
  `showProfileSwitcher` and `bottomBar` props for the phone-only chrome; `ProfileSwitcher` and `MobileNav` are
  `md:hidden`. The Android app is the visual reference for the phone layout — keep them in step.

Auth is Google sign-in (`signInWithPopup`). `crypto.randomUUID()` generates doc ids; the default profile id
is `"default"` (matches Android's `DEFAULT_PROFILE_ID`).

`useEntries` fetches **all** entry docs in each collection (no server-side `where`) and filters by the active
`profileId` + `isDeleted == false` **client-side**, defaulting a missing `profileId` to `"default"`. This is
deliberate: a server-side `where('profileId','==',…)` equality filter silently excludes pre-multi-profile docs
that have no `profileId` field (Android tolerates them via `?: DEFAULT_PROFILE_ID` on pull — the webapp must
mirror that, or older history disappears). No composite Firestore index is required either way.

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
