package com.mountaincrab.logrhythm.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import com.mountaincrab.logrhythm.data.repository.FoodEntryLineInput
import com.mountaincrab.logrhythm.data.repository.FoodRepository
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

/**
 * Logs the tapped tile's shortcut, at the moment of the tap.
 *
 * One tap is the whole feature, so this deliberately has no confirmation step: a mis-tap is
 * undone by deleting the entry, the same gesture every other entry type uses. What it does owe
 * the user is proof it happened — a toast now, and a "Logged HH:mm" stamp on the tile that
 * survives it.
 *
 * A tile that has no resolvable config never routes here at all — it carries an activity
 * intent instead, because the launcher can start setup where a broadcast cannot. The check
 * below is only for the race where the config disappears between render and tap.
 */
class QuickAddLogAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val koin = GlobalContext.get()
        val preferences: UserPreferencesRepository = koin.get()
        val foodRepository: FoodRepository = koin.get()

        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val config = preferences.getQuickAddWidgetConfig(appWidgetId)
        val item = config?.let { foodRepository.getFoodItemInProfile(it.profileId, it.foodItemId) }

        if (config == null || item == null) return

        val occurredAt = currentTimeMillis()
        foodRepository.saveEntry(
            occurredAt = occurredAt,
            mealTag = null,
            inputs = listOf(FoodEntryLineInput(foodItemId = item.id, quantity = config.quantity)),
            profileIdOverride = config.profileId,
        )
        preferences.setQuickAddWidgetLoggedAt(appWidgetId, occurredAt)

        // The tile itself re-renders from the stamp it just wrote; the toast is what confirms
        // the tap before the user looks away.
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Logged ${item.name}", Toast.LENGTH_SHORT).show()
        }
    }
}
