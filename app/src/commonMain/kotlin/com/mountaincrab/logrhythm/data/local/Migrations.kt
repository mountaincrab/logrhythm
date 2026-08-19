package com.mountaincrab.logrhythm.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Room migration registry. Add Migration(oldVer, newVer) { ... }
 * entries here every time the @Database version bumps.
 *
 * Bump procedure (from CLAUDE.md):
 * 1. Change entity, bump @Database(version = N).
 * 2. ./gradlew :app:compileDebugKotlinAndroid — emits schema JSON.
 * 3. Diff N-1.json vs N.json, add Migration here.
 *
 * Room 2.7 + BundledSQLiteDriver: migrations MUST override
 * migrate(SQLiteConnection) and use the androidx.sqlite.execSQL extension.
 * Overriding migrate(SupportSQLiteDatabase) compiles fine but throws
 * NotImplementedError on-device the first time a migration runs. The reflection
 * test in MigrationTest guards this (MigrationTestHelper alone won't catch it).
 */

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        // Recreate poop_entries: bristolTypes TEXT → INTEGER (bitmask)
        connection.execSQL("""
            CREATE TABLE poop_entries_new (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL DEFAULT 'local',
                occurredAt INTEGER NOT NULL,
                bristolTypes INTEGER NOT NULL DEFAULT 0,
                blood INTEGER NOT NULL,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        // Migrate existing rows; old bristolTypes CSV becomes 0 (no selection)
        connection.execSQL("""
            INSERT INTO poop_entries_new
                (id, userId, occurredAt, bristolTypes, blood, notes, createdAt, updatedAt, syncStatus, isDeleted)
            SELECT id, userId, occurredAt, 0, blood, notes, createdAt, updatedAt, syncStatus, isDeleted
            FROM poop_entries
        """.trimIndent())
        connection.execSQL("DROP TABLE poop_entries")
        connection.execSQL("ALTER TABLE poop_entries_new RENAME TO poop_entries")

        // User-defined stool tags (later renamed to poop_tags in migration 5→6)
        connection.execSQL("""
            CREATE TABLE stool_tags (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // Junction table: poop entries ↔ stool tags (later renamed in migration 5→6)
        connection.execSQL("""
            CREATE TABLE poop_entry_stool_tags (
                entryId TEXT NOT NULL,
                tagId TEXT NOT NULL,
                PRIMARY KEY (entryId, tagId)
            )
        """.trimIndent())
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // Rename poop-entry tag tables
        connection.execSQL("ALTER TABLE stool_tags RENAME TO poop_tags")
        connection.execSQL("ALTER TABLE poop_entry_stool_tags RENAME TO poop_entry_tag_refs")

        // Rename note-entry tag tables
        connection.execSQL("ALTER TABLE extras_tags RENAME TO note_tags")
        connection.execSQL("ALTER TABLE note_entry_extras_tags RENAME TO note_entry_tag_refs")
    }
}

// ALTER TABLE can't add NOT NULL columns without a DEFAULT, but Room 2.7 compares
// default values strictly. Use recreate-table so the schema matches entity definitions exactly.
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE poop_tags_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isDeleted INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO poop_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, 0, 'PENDING' FROM poop_tags
        """.trimIndent())
        connection.execSQL("DROP TABLE poop_tags")
        connection.execSQL("ALTER TABLE poop_tags_new RENAME TO poop_tags")

        connection.execSQL("""
            CREATE TABLE note_tags_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isDeleted INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO note_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, 0, 'PENDING' FROM note_tags
        """.trimIndent())
        connection.execSQL("DROP TABLE note_tags")
        connection.execSQL("ALTER TABLE note_tags_new RENAME TO note_tags")
    }
}

// Fixes devices that ran the original MIGRATION_6_7 (which used ALTER TABLE and produced
// columns with DEFAULT clauses that Room 2.7 schema validation rejects).
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE poop_tags_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isDeleted INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO poop_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus FROM poop_tags
        """.trimIndent())
        connection.execSQL("DROP TABLE poop_tags")
        connection.execSQL("ALTER TABLE poop_tags_new RENAME TO poop_tags")

        connection.execSQL("""
            CREATE TABLE note_tags_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isDeleted INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO note_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus FROM note_tags
        """.trimIndent())
        connection.execSQL("DROP TABLE note_tags")
        connection.execSQL("ALTER TABLE note_tags_new RENAME TO note_tags")
    }
}

// Adds local sub-profiles: a profiles table + profileId on every per-profile table.
// profileId uses @ColumnInfo(defaultValue = "default") on the entities so the ALTER TABLE
// DEFAULT matches Room 2.7's strict default-value validation.
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        val now = System.currentTimeMillis()

        // profiles table — no column DEFAULTs, to match ProfileEntity exactly.
        connection.execSQL("""
            CREATE TABLE profiles (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                theme TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())

        // Default profile that owns all pre-existing data.
        connection.execSQL(
            "INSERT INTO profiles (id, name, theme, createdAt, updatedAt, syncStatus, isDeleted) " +
                "VALUES ('default', 'Me', 'DEEP_NAVY', $now, $now, 'PENDING', 0)"
        )

        // Backfill profileId on every per-profile table (existing rows → default profile).
        connection.execSQL("ALTER TABLE poop_entries ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE food_entries ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE note_entries ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE poop_tags ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE note_tags ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
    }
}

// Adds medication as a first-class entry type: a catalog of medications, the scheduled
// doses that reference them, and the recorded doses that land on the timeline.
// Pure table creation — no existing table is touched, so there is nothing to backfill.
private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE medications (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                name TEXT NOT NULL,
                form TEXT NOT NULL,
                defaultAmount TEXT NOT NULL,
                defaultUnit TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())

        connection.execSQL("""
            CREATE TABLE medication_schedules (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                medicationId TEXT NOT NULL,
                amount TEXT NOT NULL,
                unit TEXT NOT NULL,
                timeMinutes INTEGER NOT NULL,
                repeatRule TEXT NOT NULL,
                daysMask INTEGER NOT NULL,
                startEpochDay INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())

        connection.execSQL("""
            CREATE TABLE medication_entries (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                medicationId TEXT NOT NULL,
                medicationName TEXT NOT NULL,
                occurredAt INTEGER NOT NULL,
                amount TEXT NOT NULL,
                unit TEXT NOT NULL,
                status TEXT NOT NULL,
                scheduleId TEXT,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

/**
 * Reshapes medication around "a medication is name + form + strength, a dose is a quantity of it".
 *
 * The three tables are dropped and recreated rather than altered: medication shipped days ago,
 * so there is no history worth the conversion logic, and a clean rebuild keeps the schema honest
 * (`amount`/`unit` split into the medication's `dose` and the dose's `quantity`; the per-dose
 * `status` is gone entirely — an entry existing is the record).
 */
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS medication_entries")
        connection.execSQL("DROP TABLE IF EXISTS medication_schedules")
        connection.execSQL("DROP TABLE IF EXISTS medications")

        connection.execSQL("""
            CREATE TABLE medications (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                name TEXT NOT NULL,
                form TEXT NOT NULL,
                dose TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())

        connection.execSQL("""
            CREATE TABLE medication_schedules (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                medicationId TEXT NOT NULL,
                quantity TEXT NOT NULL,
                timeMinutes INTEGER NOT NULL,
                repeatRule TEXT NOT NULL,
                daysMask INTEGER NOT NULL,
                startEpochDay INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())

        connection.execSQL("""
            CREATE TABLE medication_entries (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                medicationId TEXT NOT NULL,
                medicationName TEXT NOT NULL,
                dose TEXT NOT NULL,
                quantity TEXT NOT NULL,
                occurredAt INTEGER NOT NULL,
                scheduleId TEXT,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

/**
 * Splits a medication's strength into `doseAmount` + `doseUnit` ("1g" → "1" + "g"), so the
 * editor can offer the number and its unit as separate fields. Everything that displays a
 * strength re-joins them, so nothing else changes shape.
 *
 * Table rebuild rather than ALTER: the old `dose` column has to go, and DROP COLUMN isn't
 * available on every SQLite version Android ships. Existing strengths move into `doseAmount`
 * whole, leaving `doseUnit` empty — they still read exactly as before, and splitting them
 * properly is a job for the editor, not a regex.
 */
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE medications_new (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                name TEXT NOT NULL,
                form TEXT NOT NULL,
                doseAmount TEXT NOT NULL,
                doseUnit TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())
        // syncStatus is forced to PENDING so the reshaped rows get pushed to Firestore.
        connection.execSQL("""
            INSERT INTO medications_new
                (id, userId, profileId, name, form, doseAmount, doseUnit, sortOrder,
                 createdAt, updatedAt, syncStatus, isDeleted)
            SELECT id, userId, profileId, name, form, dose, '', sortOrder,
                   createdAt, updatedAt, 'PENDING', isDeleted
            FROM medications
        """.trimIndent())
        connection.execSQL("DROP TABLE medications")
        connection.execSQL("ALTER TABLE medications_new RENAME TO medications")
    }
}

/**
 * Makes the medication catalog the single source of truth for a dose's name and strength.
 *
 * `medication_entries` drops its `medicationName` / `dose` snapshots: both are now read
 * through `medicationId` at display time, so correcting a definition corrects every dose of
 * it and any field added to the catalog later needs no back-fill.
 *
 * That only holds if the lookup can never miss, so `medications` and `medication_schedules`
 * rename `isDeleted` to `isArchived`: neither is ever really removed, and a flag that must
 * never be read as "gone" shouldn't be named that. Entries keep `isDeleted` — those genuinely
 * are deletable.
 *
 * Table rebuilds rather than ALTERs: DROP COLUMN and RENAME COLUMN aren't available on every
 * SQLite version Android ships. syncStatus is forced to PENDING so reshaped rows get pushed.
 */
private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE medications_new (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                name TEXT NOT NULL,
                form TEXT NOT NULL,
                doseAmount TEXT NOT NULL,
                doseUnit TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isArchived INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO medications_new
                (id, userId, profileId, name, form, doseAmount, doseUnit, sortOrder,
                 createdAt, updatedAt, syncStatus, isArchived)
            SELECT id, userId, profileId, name, form, doseAmount, doseUnit, sortOrder,
                   createdAt, updatedAt, 'PENDING', isDeleted
            FROM medications
        """.trimIndent())
        connection.execSQL("DROP TABLE medications")
        connection.execSQL("ALTER TABLE medications_new RENAME TO medications")

        connection.execSQL("""
            CREATE TABLE medication_schedules_new (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                medicationId TEXT NOT NULL,
                quantity TEXT NOT NULL,
                timeMinutes INTEGER NOT NULL,
                repeatRule TEXT NOT NULL,
                daysMask INTEGER NOT NULL,
                startEpochDay INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isArchived INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO medication_schedules_new
                (id, userId, profileId, medicationId, quantity, timeMinutes, repeatRule,
                 daysMask, startEpochDay, isActive, createdAt, updatedAt, syncStatus, isArchived)
            SELECT id, userId, profileId, medicationId, quantity, timeMinutes, repeatRule,
                   daysMask, startEpochDay, isActive, createdAt, updatedAt, 'PENDING', isDeleted
            FROM medication_schedules
        """.trimIndent())
        connection.execSQL("DROP TABLE medication_schedules")
        connection.execSQL("ALTER TABLE medication_schedules_new RENAME TO medication_schedules")

        connection.execSQL("""
            CREATE TABLE medication_entries_new (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                profileId TEXT NOT NULL,
                medicationId TEXT NOT NULL,
                quantity TEXT NOT NULL,
                occurredAt INTEGER NOT NULL,
                scheduleId TEXT,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                isDeleted INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO medication_entries_new
                (id, userId, profileId, medicationId, quantity, occurredAt, scheduleId,
                 notes, createdAt, updatedAt, syncStatus, isDeleted)
            SELECT id, userId, profileId, medicationId, quantity, occurredAt, scheduleId,
                   notes, createdAt, updatedAt, 'PENDING', isDeleted
            FROM medication_entries
        """.trimIndent())
        connection.execSQL("DROP TABLE medication_entries")
        connection.execSQL("ALTER TABLE medication_entries_new RENAME TO medication_entries")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_3_4, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
)
