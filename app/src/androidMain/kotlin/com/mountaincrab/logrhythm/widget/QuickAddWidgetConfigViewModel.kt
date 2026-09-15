package com.mountaincrab.logrhythm.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import com.mountaincrab.logrhythm.data.repository.FoodRepository
import com.mountaincrab.logrhythm.data.repository.ProfileRepository
import com.mountaincrab.logrhythm.preferences.QuickAddWidgetConfig
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class QuickAddWidgetConfigViewModel(
    private val context: Context,
    private val foodRepository: FoodRepository,
    private val profileRepository: ProfileRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /** The active profile's live catalogue — a widget is configured for the profile you are in. */
    val foodItems: StateFlow<List<FoodItemEntity>> = foodRepository.observeFoodItems()
        .map { items -> items.map { it.item } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** What this tile is set to today — reconfiguring an existing widget should open on its
     *  own choice rather than silently resetting it to the first item and one serving. */
    suspend fun existingConfig(appWidgetId: Int): QuickAddWidgetConfig? =
        preferences.getQuickAddWidgetConfig(appWidgetId)

    suspend fun save(appWidgetId: Int, foodItemId: String, quantity: Double) {
        preferences.setQuickAddWidgetConfig(
            appWidgetId,
            QuickAddWidgetConfig(
                profileId = profileRepository.activeProfileId.value,
                foodItemId = foodItemId,
                quantity = quantity,
            ),
        )
        // A freshly placed widget has no running Glance session, so the first render has to be
        // asked for explicitly — without this the tile sits on its loading layout until the
        // launcher next updates it.
        val manager = GlanceAppWidgetManager(context)
        runCatching { manager.getGlanceIdBy(appWidgetId) }
            .getOrNull()
            ?.let { QuickAddFoodWidget().update(context, it) }
    }
}
