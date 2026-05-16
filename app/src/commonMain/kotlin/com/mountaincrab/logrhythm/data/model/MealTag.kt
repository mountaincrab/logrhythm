package com.mountaincrab.logrhythm.data.model

enum class MealTag(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    DRINK("Drink");

    companion object {
        fun fromName(name: String?): MealTag? =
            entries.firstOrNull { it.name == name }
    }
}
