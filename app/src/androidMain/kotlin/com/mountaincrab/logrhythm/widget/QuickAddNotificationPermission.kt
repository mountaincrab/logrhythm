package com.mountaincrab.logrhythm.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Why an app that posts no notifications asks for the notification permission.
 *
 * The quick-add tile's confirmation is a toast, and a toast posted from the background is
 * dropped by the system whenever the app's notifications are off — which, from Android 13, is
 * every app that has never asked. The permission therefore buys exactly one thing here: the
 * "Added 1 Tea" banner after a tap. Nothing is ever posted to the notification shade, and a
 * refusal costs only the banner, because the tile's own `✓ HH:mm` stamp still records the tap.
 *
 * So it is asked for only where it has been earned — right after a tile is configured, or on
 * opening an app that already has one on the launcher — and [UserPreferencesRepository]'s
 * one-shot flag keeps it to a single dialog: asking twice is how a permission gets denied
 * permanently, and this one is worth less than the feature it belongs to.
 */
object QuickAddNotificationPermission {

    /** The constant rather than `Manifest.permission`, which only resolves from API 33. */
    const val PERMISSION = "android.permission.POST_NOTIFICATIONS"

    /** True when a toast would currently be suppressed and asking could change that. */
    fun isNeeded(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED

    /** Whether any quick-add tile is on a launcher — nothing else in the app wants this. */
    fun hasPlacedWidgets(context: Context): Boolean =
        runCatching {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, QuickAddFoodWidgetReceiver::class.java))
                .isNotEmpty()
        }.getOrDefault(false)
}
