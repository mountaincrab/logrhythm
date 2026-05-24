package com.mountaincrab.logrhythm.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration registry. Add Migration(oldVer, newVer) { ... }
 * entries here every time the @Database version bumps.
 *
 * Bump procedure (from CLAUDE.md):
 * 1. Change entity, bump @Database(version = N).
 * 2. ./gradlew :app:compileDebugKotlinAndroid — emits schema JSON.
 * 3. Diff N-1.json vs N.json, add Migration here.
 */

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recreate poop_entries: bristolTypes TEXT → INTEGER (bitmask)
        db.execSQL("""
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
        db.execSQL("""
            INSERT INTO poop_entries_new
                (id, userId, occurredAt, bristolTypes, blood, notes, createdAt, updatedAt, syncStatus, isDeleted)
            SELECT id, userId, occurredAt, 0, blood, notes, createdAt, updatedAt, syncStatus, isDeleted
            FROM poop_entries
        """.trimIndent())
        db.execSQL("DROP TABLE poop_entries")
        db.execSQL("ALTER TABLE poop_entries_new RENAME TO poop_entries")

        // User-defined stool tags (later renamed to poop_tags in migration 5→6)
        db.execSQL("""
            CREATE TABLE stool_tags (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // Junction table: poop entries ↔ stool tags (later renamed in migration 5→6)
        db.execSQL("""
            CREATE TABLE poop_entry_stool_tags (
                entryId TEXT NOT NULL,
                tagId TEXT NOT NULL,
                PRIMARY KEY (entryId, tagId)
            )
        """.trimIndent())
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Rename poop-entry tag tables
        db.execSQL("ALTER TABLE stool_tags RENAME TO poop_tags")
        db.execSQL("ALTER TABLE poop_entry_stool_tags RENAME TO poop_entry_tag_refs")

        // Rename note-entry tag tables
        db.execSQL("ALTER TABLE extras_tags RENAME TO note_tags")
        db.execSQL("ALTER TABLE note_entry_extras_tags RENAME TO note_entry_tag_refs")
    }
}

// ALTER TABLE can't add NOT NULL columns without a DEFAULT, but Room 2.7 compares
// default values strictly. Use recreate-table so the schema matches entity definitions exactly.
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
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
        db.execSQL("""
            INSERT INTO poop_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, 0, 'PENDING' FROM poop_tags
        """.trimIndent())
        db.execSQL("DROP TABLE poop_tags")
        db.execSQL("ALTER TABLE poop_tags_new RENAME TO poop_tags")

        db.execSQL("""
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
        db.execSQL("""
            INSERT INTO note_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, 0, 'PENDING' FROM note_tags
        """.trimIndent())
        db.execSQL("DROP TABLE note_tags")
        db.execSQL("ALTER TABLE note_tags_new RENAME TO note_tags")
    }
}

// Fixes devices that ran the original MIGRATION_6_7 (which used ALTER TABLE and produced
// columns with DEFAULT clauses that Room 2.7 schema validation rejects).
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
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
        db.execSQL("""
            INSERT INTO poop_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus FROM poop_tags
        """.trimIndent())
        db.execSQL("DROP TABLE poop_tags")
        db.execSQL("ALTER TABLE poop_tags_new RENAME TO poop_tags")

        db.execSQL("""
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
        db.execSQL("""
            INSERT INTO note_tags_new (id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus)
            SELECT id, name, isDeleted, sortOrder, createdAt, updatedAt, syncStatus FROM note_tags
        """.trimIndent())
        db.execSQL("DROP TABLE note_tags")
        db.execSQL("ALTER TABLE note_tags_new RENAME TO note_tags")
    }
}

// Adds local sub-profiles: a profiles table + profileId on every per-profile table.
// profileId uses @ColumnInfo(defaultValue = "default") on the entities so the ALTER TABLE
// DEFAULT matches Room 2.7's strict default-value validation.
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // profiles table — no column DEFAULTs, to match ProfileEntity exactly.
        db.execSQL("""
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
        db.execSQL(
            "INSERT INTO profiles (id, name, theme, createdAt, updatedAt, syncStatus, isDeleted) " +
                "VALUES ('default', 'Me', 'DEEP_NAVY', $now, $now, 'PENDING', 0)"
        )

        // Backfill profileId on every per-profile table (existing rows → default profile).
        db.execSQL("ALTER TABLE poop_entries ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("ALTER TABLE note_entries ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("ALTER TABLE poop_tags ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("ALTER TABLE note_tags ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
