package com.mountaincrab.logrhythm.data.model

/**
 * How stool type is displayed in the UI. Stored as a string in DataStore.
 */
enum class StoolSystem(val displayName: String, val sub: String) {
    BRISTOL("Bristol scale", "Medical standard, 7 types with diagrams"),
    PLAIN("My plain types",   "Mushy, soft lumps, hard lumps…"),
    BOTH("Both",              "Show Bristol number + your label side-by-side");

    companion object {
        fun fromName(name: String?): StoolSystem =
            entries.firstOrNull { it.name == name } ?: BRISTOL
    }
}
