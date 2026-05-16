package com.mountaincrab.logrhythm.data.local

import androidx.room.migration.Migration

/**
 * Room migration registry. Empty on v1; add Migration(oldVer, newVer) { ... }
 * entries here every time the @Database version bumps.
 *
 * See README/CLAUDE.md (once written) for the bump procedure:
 * 1. Make the entity change, increment version.
 * 2. ./gradlew :app:compileDebugKotlinAndroid — emits new schema JSON.
 * 3. Diff old vs new JSON and add the Migration here.
 *
 * Without a matching migration, the app crashes on upgrade. This is intentional —
 * fallbackToDestructiveMigrationOnDowngrade is enabled but not for upgrades, so
 * we never silently drop user data.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    Migration(1, 2) { db ->
        db.execSQL("ALTER TABLE poop_entries ADD COLUMN bristolTypes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE note_entries ADD COLUMN medsMissed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE note_entries ADD COLUMN caffeine INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE note_entries ADD COLUMN alcohol INTEGER NOT NULL DEFAULT 0")
    },
)
