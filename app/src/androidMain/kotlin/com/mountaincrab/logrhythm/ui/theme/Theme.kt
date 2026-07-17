package com.mountaincrab.logrhythm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppTheme(val displayName: String) {
    DEEP_NAVY("Deep Navy"),
    CHARCOAL("Charcoal"),
    RETRO("Retro");

    companion object {
        fun fromName(name: String?): AppTheme =
            entries.firstOrNull { it.name == name } ?: DEEP_NAVY
    }
}

val AccentBlue = Color(0xFF4F7CFF)
val AccentPurple = Color(0xFF8B5CF6)
val AccentCyan = Color(0xFF06B6D4)
val AccentRed = Color(0xFFEF4444)
val AccentGreen = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentOrange = Color(0xFFF97316)

// Rating colours 1..5 (green → red) — match phone.jsx RATING_COLORS.
data class RatingColor(val bg: Color, val fg: Color, val soft: Color, val label: String)
val RatingColors = mapOf(
    1 to RatingColor(Color(0xFF24FA07), Color(0xFF0A0A0A),  Color(0x2924FA07), "No blood"),
    2 to RatingColor(Color(0xFFFACC15), Color(0xFF0A0A0A),  Color(0x29FACC15), "Trace"),
    3 to RatingColor(Color(0xFFF97316), Color.White,        Color(0x2EF97316), "Small amount"),
    4 to RatingColor(Color(0xFFE64A19), Color.White,        Color(0x2EE64A19), "Quite a lot"),
    5 to RatingColor(Color(0xFFDC2626), Color.White,        Color(0x33DC2626), "Loads"),
)

// Surface ladder + extra semantic colours not covered by Material3.
data class AppPalette(
    val surfaceRaised: Color,    // cards, list rows         (--surface-raised)
    val surfaceHigh: Color,      // inputs, chips, hover     (--surface-high)
    val border: Color,           // 1px borders              (--border)
    val borderSubtle: Color,     // subtle dividers          (--border-subtle)
    val borderStrong: Color,
    val fgMuted: Color,          // secondary text           (--fg-muted)
    val fgFaint: Color,          // tertiary text            (--fg-faint)
    val fgDisabled: Color,
    val accentText: Color,       // lighter accent, inline   (--accent-text)
    val accentSoft: Color,       // ~18% accent              (--accent-soft)
    val gradientStart: Color,
    val gradientEnd: Color,
    val successText: Color,
    val dangerText: Color,
    val warning: Color,
)

val LocalAppPalette = compositionLocalOf {
    AppPalette(
        surfaceRaised = Color(0xFF1C2340),
        surfaceHigh = Color(0xFF2A3250),
        border = Color(0x1AFFFFFF),
        borderSubtle = Color(0x0DFFFFFF),
        borderStrong = Color(0x33FFFFFF),
        fgMuted = Color(0xFF9CA3AF),
        fgFaint = Color(0xFF64748B),
        fgDisabled = Color(0xFF475569),
        accentText = Color(0xFFA5B4FC),
        accentSoft = Color(0x2E4F7CFF),
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        successText = Color(0xFF34D399),
        dangerText = Color(0xFFF87171),
        warning = Color(0xFFFBBF24),
    )
}

@Composable
fun accentGradient(): Brush {
    val p = LocalAppPalette.current
    return Brush.linearGradient(listOf(p.gradientStart, p.gradientEnd))
}

private fun buildScheme(
    primary: Color,
    secondary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    outline: Color,
) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.18f),
    onPrimaryContainer = Color.White,
    secondary = secondary,
    onSecondary = Color.White,
    secondaryContainer = secondary.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRed.copy(alpha = 0.18f),
    onErrorContainer = Color(0xFFFCA5A5),
    background = background,
    onBackground = Color(0xFFE5E7EB),
    surface = surface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = outline,
    outlineVariant = outline.copy(alpha = 0.4f),
)

private val DeepNavyScheme = buildScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    background = Color(0xFF0A1020),
    surface = Color(0xFF131A2E),
    surfaceVariant = Color(0xFF1C2340),
    outline = Color(0xFF2A3250),
)

private val CharcoalScheme = buildScheme(
    primary = AccentCyan,
    secondary = Color(0xFF3B82F6),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF1E1E1E),
    outline = Color(0xFF2A2A2A),
)

private val RetroMagenta = Color(0xFFFF00CC)
private val RetroCyan = Color(0xFF00FFEE)
private val RetroYellow = Color(0xFFFFEE00)

private val RetroScheme = darkColorScheme(
    primary = RetroMagenta,
    onPrimary = Color.Black,
    primaryContainer = RetroMagenta.copy(alpha = 0.20f),
    onPrimaryContainer = Color.White,
    secondary = RetroCyan,
    onSecondary = Color.Black,
    secondaryContainer = RetroCyan.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,
    tertiary = RetroYellow,
    onTertiary = Color.Black,
    error = Color(0xFFFF4400),
    onError = Color.White,
    background = Color(0xFF1A0B1E),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF29153A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2E1438),
    onSurfaceVariant = Color(0xFFDCC4EC),
    outline = Color(0x38FF00CC),
    outlineVariant = Color(0x1AFF00CC),
)

private fun paletteFor(theme: AppTheme): AppPalette = when (theme) {
    AppTheme.DEEP_NAVY -> AppPalette(
        surfaceRaised = Color(0xFF1C2340),
        surfaceHigh = Color(0xFF2A3250),
        border = Color(0x1AFFFFFF),
        borderSubtle = Color(0x0DFFFFFF),
        borderStrong = Color(0x33FFFFFF),
        fgMuted = Color(0xFF9CA3AF),
        fgFaint = Color(0xFF64748B),
        fgDisabled = Color(0xFF475569),
        accentText = Color(0xFFA5B4FC),
        accentSoft = Color(0x2E4F7CFF),
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        successText = Color(0xFF34D399),
        dangerText = Color(0xFFF87171),
        warning = Color(0xFFFBBF24),
    )
    AppTheme.CHARCOAL -> AppPalette(
        surfaceRaised = Color(0xFF1E1E1E),
        surfaceHigh = Color(0xFF2A2A2A),
        border = Color(0x1AFFFFFF),
        borderSubtle = Color(0x0DFFFFFF),
        borderStrong = Color(0x33FFFFFF),
        fgMuted = Color(0xFFA1A1AA),
        fgFaint = Color(0xFF71717A),
        fgDisabled = Color(0xFF52525B),
        accentText = Color(0xFF67E8F9),
        accentSoft = Color(0x2E06B6D4),
        gradientStart = AccentCyan,
        gradientEnd = Color(0xFF3B82F6),
        successText = Color(0xFF34D399),
        dangerText = Color(0xFFF87171),
        warning = Color(0xFFFBBF24),
    )
    AppTheme.RETRO -> AppPalette(
        surfaceRaised = Color(0xFF2E1438),
        surfaceHigh = Color(0xFF3F1F50),
        border = Color(0x38FF00CC),
        borderSubtle = Color(0x1AFF00CC),
        borderStrong = Color(0x66FF00CC),
        fgMuted = Color(0xFFDCC4EC),
        fgFaint = Color(0xFFA584C0),
        fgDisabled = Color(0xFF6B5586),
        accentText = Color(0xFFFF66DD),
        accentSoft = Color(0x2EFF00CC),
        gradientStart = RetroMagenta,
        gradientEnd = RetroCyan,
        successText = Color(0xFF66FFF5),
        dangerText = Color(0xFFFF7755),
        warning = RetroYellow,
    )
}

private val LogRhythmTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.68).sp),
    displayMedium = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.0).sp),
    displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
)

@Composable
fun LogRhythmTheme(
    appTheme: AppTheme = AppTheme.DEEP_NAVY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.DEEP_NAVY -> DeepNavyScheme
        AppTheme.CHARCOAL -> CharcoalScheme
        AppTheme.RETRO -> RetroScheme
    }
    val palette = paletteFor(appTheme)

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LogRhythmTypography,
            content = content
        )
    }
}
