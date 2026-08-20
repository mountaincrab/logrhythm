package com.mountaincrab.logrhythm.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mountaincrab.logrhythm.data.model.MedicationForm

/**
 * The medication icon set: one general mark plus one icon per [MedicationForm].
 *
 * These replace the emoji this app used to print, for two reasons. There is no emoji for
 * granules, foam, an enema or a suppository, so five forms shared two glyphs — and the one
 * doing most of the work was the pill, which is also what "medicine" itself was drawn as.
 * The general mark is a bottle precisely because it is the one shape in the set that isn't
 * a tablet, so the timeline can say "a dose" and "which form" in the same row without the
 * two looking alike.
 *
 * Each icon is a list of filled paths on a 32x32 viewport, and the same path data backs the
 * web app's `MedicationIcons.tsx`. They carry their own colours rather than taking a palette
 * tint: they sit beside the poop/food/note emoji, so they have to read as part of that
 * family on all three themes rather than as chrome. Keep the two files in step — a shape
 * changed here and not there is a difference the user sees when they switch device.
 */
object MedicationIcons {

    val General: ImageVector by lazy { build("MedicationGeneral", GENERAL) }

    /** Icons for every form, archived ones included — a dose must always resolve one. */
    fun of(form: MedicationForm): ImageVector = byForm.getValue(form)

    private val byForm: Map<MedicationForm, ImageVector> by lazy {
        mapOf(
            MedicationForm.TABLET to build("MedicationTablet", TABLET),
            MedicationForm.GRANULES to build("MedicationGranules", GRANULES),
            MedicationForm.FOAM to build("MedicationFoam", FOAM),
            MedicationForm.ENEMA to build("MedicationEnema", ENEMA),
            MedicationForm.SUPPOSITORY to build("MedicationSuppository", SUPPOSITORY),
        )
    }

    private class Part(val path: String, val fill: Long, val alpha: Float)

    private fun part(path: String, fill: Long, alpha: Float = 1f) = Part(path, fill, alpha)

    private fun build(name: String, parts: List<Part>): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = SIZE.dp,
            defaultHeight = SIZE.dp,
            viewportWidth = SIZE,
            viewportHeight = SIZE,
        ).apply {
            parts.forEach { p ->
                addPath(
                    pathData = PathParser().parsePathString(p.path).toNodes(),
                    fill = SolidColor(Color(p.fill)),
                    fillAlpha = p.alpha,
                )
            }
        }.build()

    private const val SIZE = 32f

    private val GENERAL = listOf(
        part("M12.5 3H19.5A2 2 0 0 1 21.5 5V6.4A2 2 0 0 1 19.5 8.4H12.5A2 2 0 0 1 10.5 6.4V5A2 2 0 0 1 12.5 3Z", 0xFF46557A),
        part("M13.05 4.2H15.75A0.85 0.85 0 0 1 16.6 5.05V5.05A0.85 0.85 0 0 1 15.75 5.9H13.05A0.85 0.85 0 0 1 12.2 5.05V5.05A0.85 0.85 0 0 1 13.05 4.2Z", 0xFF7C8CB2),
        part("M11.5 8H20.5A4.5 4.5 0 0 1 25 12.5V24.5A4.5 4.5 0 0 1 20.5 29H11.5A4.5 4.5 0 0 1 7 24.5V12.5A4.5 4.5 0 0 1 11.5 8Z", 0xFFF5A623),
        part("M18 8h2.5A4.5 4.5 0 0 1 25 12.5v12A4.5 4.5 0 0 1 20.5 29H18Z", 0xFFDA8613),
        part("M15.2 13H16.8A1.2 1.2 0 0 1 18 14.2V22.8A1.2 1.2 0 0 1 16.8 24H15.2A1.2 1.2 0 0 1 14 22.8V14.2A1.2 1.2 0 0 1 15.2 13Z", 0xFFFFFFFF),
        part("M11.7 16.5H20.3A1.2 1.2 0 0 1 21.5 17.7V19.3A1.2 1.2 0 0 1 20.3 20.5H11.7A1.2 1.2 0 0 1 10.5 19.3V17.7A1.2 1.2 0 0 1 11.7 16.5Z", 0xFFFFFFFF),
    )

    private val TABLET = listOf(
        part("M4 16a12 12 0 1 0 24 0a12 12 0 1 0 -24 0Z", 0xFFA9BBDF),
        part("M5 16a11 11 0 1 0 22 0a11 11 0 1 0 -22 0Z", 0xFFEEF3FF),
        part("M16 5a11 11 0 0 1 0 22Z", 0xFFC3D4F2),
        part("M16 5.7H16A1.3 1.3 0 0 1 17.3 7V25A1.3 1.3 0 0 1 16 26.3H16A1.3 1.3 0 0 1 14.7 25V7A1.3 1.3 0 0 1 16 5.7Z", 0xFF8CA3CE),
        part("M9 10.8a2.4 1.7 0 1 0 4.8 0a2.4 1.7 0 1 0 -4.8 0Z", 0xFFFFFFFF, alpha = 0.9f),
    )

    private val GRANULES = listOf(
        part("M6.9 4H25.1A1.4 1.4 0 0 1 26.5 5.4V7.2A1.4 1.4 0 0 1 25.1 8.6H6.9A1.4 1.4 0 0 1 5.5 7.2V5.4A1.4 1.4 0 0 1 6.9 4Z", 0xFF1D7A63),
        part("M7.5 8.6h17v15.9a3.5 3.5 0 0 1-3.5 3.5h-10a3.5 3.5 0 0 1-3.5-3.5Z", 0xFF2FA98A),
        part("M18 8.6h6.5v15.9a3.5 3.5 0 0 1-3.5 3.5H18Z", 0xFF23896E),
        part("M10.8 14.2a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z", 0xFFF5C84E),
        part("M15.6 17.6a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z", 0xFFF5C84E),
        part("M11.6 21.4a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z", 0xFFF5C84E),
        part("M18.9 12.8a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0Z", 0xFFF5C84E),
    )

    private val FOAM = listOf(
        part("M14.2 2.8H17.8A1.2 1.2 0 0 1 19 4V5A1.2 1.2 0 0 1 17.8 6.2H14.2A1.2 1.2 0 0 1 13 5V4A1.2 1.2 0 0 1 14.2 2.8Z", 0xFF95A1F5),
        part("M14.2 6H17.8A4.4 4.4 0 0 1 22.2 10.4V24.6A4.4 4.4 0 0 1 17.8 29H14.2A4.4 4.4 0 0 1 9.8 24.6V10.4A4.4 4.4 0 0 1 14.2 6Z", 0xFF6C7BE8),
        part("M16 6h1.8a4.4 4.4 0 0 1 4.4 4.4v14.2A4.4 4.4 0 0 1 17.8 29H16Z", 0xFF4E5CC9),
        part("M9.8 13.8H22.2V17.4H9.8Z", 0xFFC3C9F8),
        part("M22.3 7.4a3.3 3.3 0 1 0 6.6 0a3.3 3.3 0 1 0 -6.6 0Z", 0xFFFFFFFF),
        part("M26.6 13a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z", 0xFFFFFFFF),
        part("M22.8 13.6a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0Z", 0xFFFFFFFF),
    )

    private val ENEMA = listOf(
        part("M16 2.6H16A1.6 1.6 0 0 1 17.6 4.2V7.4A1.6 1.6 0 0 1 16 9H16A1.6 1.6 0 0 1 14.4 7.4V4.2A1.6 1.6 0 0 1 16 2.6Z", 0xFFF5B3C7),
        part("M14.2 8H17.8A1.4 1.4 0 0 1 19.2 9.4V9.4A1.4 1.4 0 0 1 17.8 10.8H14.2A1.4 1.4 0 0 1 12.8 9.4V9.4A1.4 1.4 0 0 1 14.2 8Z", 0xFFC94F72),
        part("M7.4 20.4a8.6 8.6 0 1 0 17.2 0a8.6 8.6 0 1 0 -17.2 0Z", 0xFFE96D91),
        part("M16 11.8a8.6 8.6 0 0 1 0 17.2Z", 0xFFCD4E74),
        part("M10.2 17a2.2 1.6 0 1 0 4.4 0a2.2 1.6 0 1 0 -4.4 0Z", 0xFFFFFFFF, alpha = 0.55f),
    )

    private val SUPPOSITORY = listOf(
        part("M10 22v-6c0-6 2.4-10.6 6-12 3.6 1.4 6 6 6 12v6a6 6 0 0 1-12 0Z", 0xFFF6D06B),
        part("M16 4c3.6 1.4 6 6 6 12v6a6 6 0 0 1-6 6Z", 0xFFDCAE3C),
        part("M11.5 12.4a1.5 2.6 0 1 0 3 0a1.5 2.6 0 1 0 -3 0Z", 0xFFFFFFFF, alpha = 0.5f),
    )
}

/** The general "a dose was taken" mark — never a specific form. */
@Composable
fun MedicineIcon(size: Dp, modifier: Modifier = Modifier) {
    Image(
        imageVector = MedicationIcons.General,
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/** The icon for one medication's physical form. */
@Composable
fun MedicationFormIcon(form: MedicationForm, size: Dp, modifier: Modifier = Modifier) {
    Image(
        imageVector = MedicationIcons.of(form),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
