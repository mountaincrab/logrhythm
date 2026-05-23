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
 * Room 2.7 + BundledSQLiteDriver: override migrate(SQLiteConnection),
 * NOT migrate(SupportSQLiteDatabase). Use androidx.sqlite.execSQL extension.
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

// Recreate-table approach: ALTER TABLE can't add NOT NULL columns without a DEFAULT,
// but Room 2.7 compares default values strictly. No DEFAULT clauses = exact schema match.
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

// Fixes devices that ran the original MIGRATION_6_7 (ALTER TABLE variant, which produced
// DEFAULT clauses that Room 2.7 schema validation rejects).
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

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
