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

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4, MIGRATION_5_6)
