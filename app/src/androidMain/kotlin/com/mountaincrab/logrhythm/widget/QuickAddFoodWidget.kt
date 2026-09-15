package com.mountaincrab.logrhythm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import com.mountaincrab.logrhythm.data.repository.FoodRepository
import com.mountaincrab.logrhythm.data.repository.ProfileRepository
import com.mountaincrab.logrhythm.preferences.QuickAddWidgetConfig
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.ui.theme.AppTheme
import com.mountaincrab.logrhythm.ui.util.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * A one-tap home-screen tile that logs a configured food item at the current time.
 *
 * Each placed widget carries its own [QuickAddWidgetConfig], so the tile is generic rather
 * than tea-shaped: drop one per shortcut you want. What it shows is the catalogue item's own
 * icon and name — those belong to the food definition and are not overridable here — plus how
 * many servings a tap logs, and when it last logged one.
 */
class QuickAddFoodWidget : GlanceAppWidget() {

    /**
     * Three breakpoints rather than one layout: at 1x1 there is only room for the icon, and a
     * name clipped to "Te..." is worse than none. Width decides, because a launcher row is what
     * gets squeezed.
     */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(50.dp, 50.dp),
            DpSize(110.dp, 50.dp),
            DpSize(180.dp, 110.dp),
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val preferences: UserPreferencesRepository = koin.get()
        val foodRepository: FoodRepository = koin.get()
        val profileRepository: ProfileRepository = koin.get()

        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val configFlow = preferences.quickAddWidgetConfig(appWidgetId)

        // Observed rather than snapshotted: renaming "Tea" or re-pointing the widget has to
        // reach the tile, the same way it reaches a historical entry line.
        val itemFlow: Flow<FoodItemEntity?> = configFlow.flatMapLatest { config ->
            if (config == null) flowOf(null)
            else foodRepository.observeFoodItem(config.profileId, config.foodItemId)
        }
        val themeFlow: Flow<AppTheme> = configFlow.flatMapLatest { config ->
            if (config == null) flowOf(AppTheme.DEEP_NAVY)
            else profileRepository.observeProfile(config.profileId).map { AppTheme.fromName(it?.theme) }
        }
        val loggedAtFlow = preferences.quickAddWidgetLoggedAt(appWidgetId)

        provideContent {
            val config by configFlow.collectAsState(initial = null)
            val item by itemFlow.collectAsState(initial = null)
            val theme by themeFlow.collectAsState(initial = AppTheme.DEEP_NAVY)
            val loggedAt by loggedAtFlow.collectAsState(initial = null)

            QuickAddTile(
                appWidgetId = appWidgetId,
                theme = QuickAddWidgetTheme.of(theme),
                config = config,
                item = item,
                loggedAt = loggedAt,
            )
        }
    }
}

@Composable
private fun QuickAddTile(
    appWidgetId: Int,
    theme: QuickAddWidgetTheme,
    config: QuickAddWidgetConfig?,
    item: FoodItemEntity?,
    loggedAt: Long?,
) {
    val context = LocalContext.current
    val width = LocalSize.current.width
    val iconOnly = width < 100.dp
    val showLoggedStamp = width >= 150.dp
    val configured = config != null && item != null

    // An unconfigured tile opens setup straight from the launcher's tap rather than routing
    // through the callback: a background activity start from a broadcast is blocked on
    // Android 10+, so the callback could not reliably get there.
    val onTap = if (configured) {
        actionRunCallback<QuickAddLogAction>()
    } else {
        actionStartActivity(
            QuickAddFoodWidgetConfigActivity.reconfigureIntent(context, appWidgetId),
        )
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(theme.backgroundRes))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable(onTap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (config == null || item == null) {
            // Unconfigured, or pointing at an item that no longer resolves — a widget restored
            // onto a wiped device, or one whose profile was deleted. Tapping opens setup (see
            // onTap above) rather than failing silently.
            Text(DEFAULT_TILE_ICON, style = TextStyle(fontSize = 24.sp))
            if (!iconOnly) {
                Text(
                    text = if (config == null) "Tap to set up" else "Item unavailable",
                    style = TextStyle(
                        color = theme.foregroundMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                )
            }
        } else {
            Text(item.icon, style = TextStyle(fontSize = if (iconOnly) 26.sp else 24.sp))
            if (!iconOnly) {
                Text(
                    text = item.name,
                    style = TextStyle(
                        color = theme.foreground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = servingLabel(config.quantity, item),
                    style = TextStyle(
                        color = theme.foregroundMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
            if (showLoggedStamp && loggedAt != null) {
                Text(
                    text = "Logged ${loggedAt.formatTime()}",
                    style = TextStyle(
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

private const val DEFAULT_TILE_ICON = "🍴"

/**
 * "2 x 250 ml" — the quantity this widget logs, against one serving as the catalogue defines
 * it. A quantity of one drops the multiplier, because "1 x 250 ml" is just "250 ml".
 */
internal fun servingLabel(quantity: Double, item: FoodItemEntity): String {
    val serving = "${item.amount} ${item.unit}".trim()
    return if (quantity == 1.0) serving else "${formatQuantity(quantity)} × $serving"
}

internal fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

class QuickAddFoodWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddFoodWidget()

    /**
     * The framework reuses widget ids, so a removed tile must not leave its config behind for
     * the next widget to inherit.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val preferences: UserPreferencesRepository = GlobalContext.get().get()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { preferences.clearQuickAddWidget(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
