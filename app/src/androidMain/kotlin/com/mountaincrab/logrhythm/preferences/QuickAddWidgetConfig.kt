package com.mountaincrab.logrhythm.preferences

import kotlinx.serialization.Serializable

/**
 * What one home-screen quick-add widget logs when it is tapped.
 *
 * Deliberately nothing but a pointer and a quantity: the icon, name and serving size are
 * read live from the catalogue item, exactly as a food entry line does, so renaming "Tea"
 * or changing its serving updates the widget instead of leaving a stale copy on the home
 * screen. It is a device preference like the Log-food quick-add tiles, not part of the
 * Firestore schema — widgets belong to the launcher they were dropped on.
 *
 * [profileId] is the profile the widget was configured for, which is not necessarily the
 * one the app currently shows: a widget must keep logging against the profile whose
 * catalogue its item came from, or the reference would not resolve.
 */
@Serializable
data class QuickAddWidgetConfig(
    val profileId: String,
    val foodItemId: String,
    val quantity: Double,
) {
    val isValid: Boolean
        get() = profileId.isNotBlank() && foodItemId.isNotBlank() && quantity.isFinite() && quantity > 0.0
}
