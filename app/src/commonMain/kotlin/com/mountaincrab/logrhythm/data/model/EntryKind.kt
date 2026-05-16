package com.mountaincrab.logrhythm.data.model

/**
 * Shared "type of entry" tag for the timeline. Each kind has its own
 * Room entity + DAO; this enum is for UI grouping only.
 */
enum class EntryKind { POOP, FOOD, NOTE }
