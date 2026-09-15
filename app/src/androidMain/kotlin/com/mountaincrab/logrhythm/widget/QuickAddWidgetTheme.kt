package com.mountaincrab.logrhythm.widget

import androidx.glance.unit.ColorProvider
import com.mountaincrab.logrhythm.R
import com.mountaincrab.logrhythm.ui.theme.AppTheme
import androidx.compose.ui.graphics.Color

/**
 * The slice of [AppTheme] a home-screen tile needs.
 *
 * Glance runs outside the app's composition, so `LocalAppPalette` is unreachable and the
 * tokens have to be restated here. Only four are: the card surface (as a rounded drawable,
 * because `cornerRadius` is API 31+ and minSdk is 26), the primary and muted text, and the
 * accent used for the logged stamp. Every value is copied from `Theme.kt` — change a palette
 * there and this has to follow, or a tile stops matching the app it belongs to.
 */
data class QuickAddWidgetTheme(
    val backgroundRes: Int,
    val foreground: ColorProvider,
    val foregroundMuted: ColorProvider,
    val accent: ColorProvider,
) {
    companion object {
        fun of(theme: AppTheme): QuickAddWidgetTheme = when (theme) {
            AppTheme.DEEP_NAVY -> QuickAddWidgetTheme(
                backgroundRes = R.drawable.widget_surface_deep_navy,
                foreground = ColorProvider(Color(0xFFF3F4F6)),
                foregroundMuted = ColorProvider(Color(0xFF9CA3AF)),
                accent = ColorProvider(Color(0xFFA5B4FC)),
            )
            AppTheme.CHARCOAL -> QuickAddWidgetTheme(
                backgroundRes = R.drawable.widget_surface_charcoal,
                foreground = ColorProvider(Color(0xFFF3F4F6)),
                foregroundMuted = ColorProvider(Color(0xFFA1A1AA)),
                accent = ColorProvider(Color(0xFF67E8F9)),
            )
            AppTheme.RETRO -> QuickAddWidgetTheme(
                backgroundRes = R.drawable.widget_surface_retro,
                foreground = ColorProvider(Color(0xFFFFFFFF)),
                foregroundMuted = ColorProvider(Color(0xFFDCC4EC)),
                accent = ColorProvider(Color(0xFFFF66DD)),
            )
        }
    }
}
