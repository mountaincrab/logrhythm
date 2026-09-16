package com.mountaincrab.logrhythm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
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
import com.mountaincrab.logrhythm.ui.util.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.time.LocalDate

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
     * Exact rather than a set of breakpoints, because the tile is laid out from the *smaller*
     * of the two dimensions and `SizeMode.Responsive` reports the matched breakpoint instead of
     * the cell's real shape. Launcher cells are routinely taller than they are wide, and a tile
     * that fills one reads as a stretched slab next to the round app icons beside it.
     */
    override val sizeMode = SizeMode.Exact

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

        // Seeded from the current values rather than from nulls: a session starts afresh every
        // time something asks for a render, and composing from nulls first would publish one
        // frame of the "Tap to set up" tile before the real one — a flash on every tap.
        val initialConfig = configFlow.first()
        val initialItem = itemFlow.first()
        val initialTheme = themeFlow.first()
        val initialLoggedAt = loggedAtFlow.first()

        provideContent {
            val config by configFlow.collectAsState(initial = initialConfig)
            val item by itemFlow.collectAsState(initial = initialItem)
            val theme by themeFlow.collectAsState(initial = initialTheme)
            val loggedAt by loggedAtFlow.collectAsState(initial = initialLoggedAt)

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
    val size = LocalSize.current
    // The tile is a square of the cell's shorter side, centred in whatever the launcher gave
    // us: that is what keeps a 1x1 shortcut looking like the app icons it sits among instead of
    // stretching to the cell's aspect ratio.
    val side = if (size.width < size.height) size.width else size.height
    val tiny = side < 56.dp
    val iconOnly = side < 100.dp
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

    // Only today's stamp is worth the room. Yesterday's time answers no question a tap asks
    // ("have I logged that tea yet?") and on an icon-only tile it is all the room there is.
    val loggedToday = loggedAt?.takeIf { it.toLocalDate() == LocalDate.now() }

    Box(
        // The whole cell is the target, not just the drawn circle: a tap that lands a couple of
        // dp outside it was still aimed at the shortcut.
        modifier = GlanceModifier.fillMaxSize().clickable(onTap),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier
                .size(side)
                // Round while it is an icon, rounded-square once it carries text — a circle
                // clips its own corners off a name.
                .background(
                    ImageProvider(
                        if (iconOnly) theme.circleBackgroundRes else theme.squareBackgroundRes,
                    ),
                )
                .padding(if (iconOnly) 4.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (config == null || item == null) {
                // Unconfigured, or pointing at an item that no longer resolves — a widget restored
                // onto a wiped device, or one whose profile was deleted. Tapping opens setup (see
                // onTap above) rather than failing silently.
                Text(DEFAULT_TILE_ICON, style = TextStyle(fontSize = iconSize(tiny, iconOnly)))
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
                Text(item.icon, style = TextStyle(fontSize = iconSize(tiny, iconOnly)))
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
                // The stamp is the feedback that cannot be suppressed: a toast fires from the
                // background, where the system drops it unless the notification permission was
                // granted, so the tile has to be able to answer for itself. It shows at every
                // size that has a line to spare — on an icon-only tile as the bare time.
                if (loggedToday != null && !tiny) {
                    Text(
                        text = if (iconOnly) {
                            "✓ ${loggedToday.formatTime()}"
                        } else {
                            "Logged ${loggedToday.formatTime()}"
                        },
                        style = TextStyle(
                            color = theme.accent,
                            fontSize = if (iconOnly) 9.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A tile too small for a second line gives the icon the room the stamp would have taken. */
private fun iconSize(tiny: Boolean, iconOnly: Boolean) = when {
    tiny -> 20.sp
    iconOnly -> 22.sp
    else -> 24.sp
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

/**
 * "Added 1 Tea" — what the tap just wrote, in the words the user would use for it. The
 * quantity leads because it is the part a tile cannot show at icon-only size, and the food's
 * name is the catalogue's, so a rename reaches the confirmation like it reaches everything else.
 */
internal fun addedLabel(quantity: Double, item: FoodItemEntity): String =
    "Added ${formatQuantity(quantity)} ${item.name}"

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
