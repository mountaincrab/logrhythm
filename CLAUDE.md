# LogRhythm — Claude guidance

LogRhythm is an Android app for tracking IBD-relevant signal: poop entries (Bristol type + blood rating + notes), food, and free-form notes. It's modeled after [crab-do](~/git/crab-do) and built to share that app's KMP + Compose + Room stack so it can later grow an iOS surface, a webapp, and a Firestore sync layer.

## Project status

- **Android only** so far. iOS / webapp / cloud functions are deliberately out of scope.
- **No Firebase, no auth.** Single local user; `userId` is hardcoded to `"local"` on every entity. `SyncStatus` columns are kept on every row so sync can be wired later without a Room migration.
- All v2 screens from `Poop tracker/` designs are implemented: Home, Add poop, Add food, Add note, History (Calendar + Trends), Entry detail, Settings.

## Stack

- Kotlin Multiplatform (Android target only)
- Jetpack Compose + Material3
- Room (KMP runtime, schemas exported to `app/schemas/`)
- Koin DI
- DataStore-Preferences (theme + stool-system pref)
- Navigation-Compose
- kotlinx-serialization (lightweight, kept for future use)

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
      local/dao/{Poop,Food,Note}EntryDao.kt, {Poop,Note}TagDao.kt
      local/entity/{Poop,Food,Note}EntryEntity.kt, {Poop,Note}TagEntity.kt, {PoopEntry,NoteEntry}TagCrossRef.kt
      model/{Bristol,EntryKind,MealTag,StoolSystem,SyncStatus}.kt
    util/Platform.kt           ← expect: currentTimeMillis(), randomUUID()
  androidMain/kotlin/com/mountaincrab/logrhythm/
    LogRhythmApplication.kt    ← Koin startup
    MainActivity.kt
    di/AppModule.kt
    data/repository/EntryRepository.kt
    preferences/UserPreferencesRepository.kt
    ui/
      theme/{Theme.kt, ThemeViewModel.kt}   ← AppTheme enum, AppPalette, RatingColors
      navigation/AppNavigation.kt           ← all routes
      components/{BottomTabBar,SheetHeader,WhenPicker,RatingPill,TimelineEntryRow}.kt
      home/{HomeViewModel,HomeScreen}.kt
      addentry/{AddPoop,AddFood,AddNote}{ViewModel,Screen}.kt
      history/{HistoryViewModel,HistoryScreen}.kt
      detail/{EntryDetailViewModel,EntryDetailScreen}.kt
      settings/{SettingsViewModel,SettingsScreen}.kt
      util/DateUtils.kt
```

## Data model

```
poop_entries            ← id, userId, occurredAt, bristolTypes (Set<Int> bitmask), blood (Int 1–5), notes?, createdAt, updatedAt, syncStatus, isDeleted
food_entries            ← id, userId, occurredAt, items (String), mealTag (MealTag?), createdAt, updatedAt, syncStatus, isDeleted
note_entries            ← id, userId, occurredAt, content (String), caffeine (Boolean), alcohol (Boolean), createdAt, updatedAt, syncStatus, isDeleted
poop_tags               ← id, name, isDeleted, sortOrder, createdAt
note_tags               ← id, name, isDeleted, sortOrder, createdAt
poop_entry_tag_refs     ← entryId, tagId  (composite PK — many-to-many join)
note_entry_tag_refs     ← entryId, tagId  (composite PK — many-to-many join)
```

Repository: `EntryRepository` (Android-only because it uses Android-style Flow combine). When we wire Firestore later, that's the place. The DAOs already expose `getUnsynced`-style queries are not yet added — keep `syncStatus = PENDING` on every write so a future SyncWorker can scan for pending rows.

## Theme

`AppTheme` enum (DEEP_NAVY, CHARCOAL, RETRO) maps to a Material3 `darkColorScheme` plus a custom `AppPalette` (provided via `LocalAppPalette` composition local) for tokens Material3 doesn't cover (`surfaceRaised`, `surfaceHigh`, `border`, `borderSubtle`, `fgMuted`, `fgFaint`, `accentText`, `accentSoft`, `successText`, `dangerText`, `warning`, gradient endpoints).

Rating colours (1..5) live in `Theme.kt:RatingColors` — mirror of `phone.jsx:RATING_COLORS`.

## Room migrations

Same convention as crab-do:

1. Change entity, bump `@Database(version = N)` in `AppDatabase.kt`.
2. `./gradlew :app:compileDebugKotlinAndroid` to emit `app/schemas/<DB>/N.json`.
3. Diff against `N-1.json` to derive SQL.
4. Add `Migration(N-1, N) { ... }` to `ALL_MIGRATIONS` in `data/local/Migrations.kt`.

Without a matching migration the app crashes on upgrade — that's the intended safety net. We use `fallbackToDestructiveMigrationOnDowngrade` only; upgrades **must** be migrated.

### CRITICAL: migrations must use `migrate(SQLiteConnection)`

The DB is built with `.setDriver(BundledSQLiteDriver())` (see `di/AppModule.kt`). With a driver, Room calls `Migration.migrate(connection: SQLiteConnection)` — **not** the old `migrate(db: SupportSQLiteDatabase)`. Each `Migration` must:

- override `migrate(connection: SQLiteConnection)`, and
- use the `androidx.sqlite.execSQL` extension: `connection.execSQL("...")`.

Overriding the `SupportSQLiteDatabase` variant **compiles fine but throws `kotlin.NotImplementedError` on-device** the first time any migration runs (the base class's `migrate(SQLiteConnection)` is a stub that throws). This shipped once already (lost during a branch merge) and crashed the app on every upgrade.

`MigrationTestHelper` runs the framework (`SupportSQLiteOpenHelper`) path, so its schema tests pass **even with the wrong signature** — a false green. The `allMigrations_overrideSQLiteConnectionMigrate()` reflection test in `MigrationTest` exists specifically to catch this; keep it.

## Designs

Source-of-truth design mockups live under `Poop tracker/` at the repo root (JSX prototype, plus reference d1/d2/d3 alternates). The implemented design is the **v2 (refined)** variant — see `Poop tracker/v2-screens.jsx` and `concepts.html`. d1 and d2 are kept only for reference.
