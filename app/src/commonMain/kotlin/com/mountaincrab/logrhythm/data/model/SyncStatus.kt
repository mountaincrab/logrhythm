package com.mountaincrab.logrhythm.data.model

/**
 * Sync state for local writes that may eventually be pushed to Firestore.
 * For now Firestore is stubbed; PENDING / SYNCED still get written so the
 * column exists and we can wire sync later without a schema migration.
 */
enum class SyncStatus { PENDING, SYNCED }
