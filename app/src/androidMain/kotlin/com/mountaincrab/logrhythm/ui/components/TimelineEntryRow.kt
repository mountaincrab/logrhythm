@file:OptIn(ExperimentalLayoutApi::class)

package com.mountaincrab.logrhythm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountaincrab.logrhythm.data.local.entity.dose
import com.mountaincrab.logrhythm.data.model.bristol
import com.mountaincrab.logrhythm.data.model.formatDoseAmount
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
import com.mountaincrab.logrhythm.preferences.HomeTimelineDensity
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.theme.RatingColors
import com.mountaincrab.logrhythm.ui.util.formatTime

/**
 * A single row on the home timeline: a coloured dot (relative to the vertical
 * line drawn by the parent) and a card with the entry contents.
 */
@Composable
fun TimelineEntryRow(
    entry: TimelineEntry,
    density: HomeTimelineDensity = HomeTimelineDensity.STANDARD,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val compact = density == HomeTimelineDensity.COMPACT
    val cardShape = RoundedCornerShape(if (compact) 10.dp else 14.dp)
    val dotColor = when (entry) {
        is TimelineEntry.Poop -> RatingColors[entry.entity.blood]?.bg ?: palette.surfaceHigh
        is TimelineEntry.Food -> palette.surfaceHigh
        is TimelineEntry.Note -> palette.warning
        is TimelineEntry.Medication -> palette.accentText
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // dot — positioned by parent's timeline padding (22dp from left).
        Box(
            modifier = Modifier
                .padding(start = if (compact) 3.dp else 2.dp, top = if (compact) 12.dp else 16.dp)
                .size(if (compact) 9.dp else 11.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
        )
        Column(
            modifier = Modifier
                .padding(start = 22.dp)
                .fillMaxWidth()
                .heightIn(min = if (compact) 40.dp else 0.dp)
                .clip(cardShape)
                .background(palette.surfaceRaised)
                .border(1.dp, palette.border, cardShape)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = if (compact) 10.dp else 14.dp,
                    vertical = if (compact) 6.dp else 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        ) {
            when (entry) {
                is TimelineEntry.Poop -> PoopBody(entry, compact)
                is TimelineEntry.Food -> FoodBody(entry, compact)
                is TimelineEntry.Note -> NoteBody(entry, compact)
                is TimelineEntry.Medication -> MedicationBody(entry, compact)
            }
        }
    }
}

@Composable
private fun PoopBody(entry: TimelineEntry.Poop, compact: Boolean) {
    val palette = LocalAppPalette.current
    val bristolNums = entry.entity.bristolTypes.sorted()
    val bristolText = buildString {
        if (bristolNums.isNotEmpty()) {
            append(bristolNums.joinToString(", "))
            val names = bristolNums.mapNotNull { runCatching { bristol(it) }.getOrNull()?.plain }
            if (names.isNotEmpty()) append(" · ${names.joinToString(", ")}")
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        Text(
            text = entry.entity.occurredAt.formatTime(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Text(
            text = "💩",
            fontSize = EntryIconSizes.timelineEmoji(compact),
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        if (bristolText.isNotEmpty()) {
            Text(
                text = bristolText,
                color = palette.fgMuted,
                fontSize = if (compact) 12.sp else 13.sp,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        RatingPill(
            rating = entry.entity.blood,
            compact = compact,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        entry.tags.forEach { tag ->
            TimelineTag(
                text = tag.name,
                compact = compact,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
    if (!entry.entity.notes.isNullOrBlank()) {
        Text(
            text = entry.entity.notes!!,
            color = palette.fgMuted,
            fontSize = if (compact) 12.sp else 14.sp,
            lineHeight = if (compact) 16.sp else 20.sp,
        )
    }
}

@Composable
private fun FoodBody(entry: TimelineEntry.Food, compact: Boolean) {
    val palette = LocalAppPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        Text(
            text = entry.entity.occurredAt.formatTime(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Text(
            text = "🍴",
            fontSize = EntryIconSizes.timelineEmoji(compact),
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
    Text(
        text = entry.entity.items,
        color = palette.fgMuted,
        fontSize = if (compact) 12.sp else 14.sp,
        lineHeight = if (compact) 16.sp else 20.sp,
    )
}

@Composable
private fun MedicationBody(entry: TimelineEntry.Medication, compact: Boolean) {
    val palette = LocalAppPalette.current
    val e = entry.entity
    // Name and strength come from the catalog row, so correcting a medication corrects
    // every dose of it. Archiving keeps the row around, so this normally resolves.
    val name = entry.medication?.name ?: "Medication"
    val amount = formatDoseAmount(e.quantity, entry.medication?.dose ?: "")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        Text(
            text = e.occurredAt.formatTime(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        MedicineIcon(
            size = EntryIconSizes.timelineIcon(compact),
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        // Name and form stay one unit — a bracket that wrapped away from what it qualifies
        // would read as belonging to the dose amount instead.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            // Absent when the medication can't be resolved — there's no form to name then.
            entry.medication?.let { med ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(text = "(", color = palette.fgFaint, fontSize = if (compact) 12.sp else 13.sp)
                    MedicationFormIcon(form = med.form, size = EntryIconSizes.timelineFormIcon(compact))
                    Text(text = ")", color = palette.fgFaint, fontSize = if (compact) 12.sp else 13.sp)
                }
            }
        }
        if (amount.isNotEmpty()) {
            Text(
                text = amount,
                color = palette.fgMuted,
                fontSize = if (compact) 12.sp else 13.sp,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
    if (!e.notes.isNullOrBlank()) {
        Text(
            text = e.notes!!,
            color = palette.fgMuted,
            fontSize = if (compact) 12.sp else 14.sp,
            lineHeight = if (compact) 16.sp else 20.sp,
        )
    }
}

@Composable
private fun NoteBody(entry: TimelineEntry.Note, compact: Boolean) {
    val palette = LocalAppPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        Text(
            text = entry.entity.occurredAt.formatTime(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Text(
            text = "📝",
            fontSize = EntryIconSizes.timelineEmoji(compact),
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        if (entry.entity.caffeine) {
            TimelineTag(
                text = "☕",
                compact = compact,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        if (entry.entity.alcohol) {
            TimelineTag(
                text = "🍺",
                compact = compact,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        entry.tags.forEach { tag ->
            TimelineTag(
                text = tag.name,
                compact = compact,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
    if (entry.entity.content.isNotBlank()) {
        Text(
            text = entry.entity.content,
            color = palette.fgMuted,
            fontSize = if (compact) 12.sp else 14.sp,
            lineHeight = if (compact) 16.sp else 20.sp,
        )
    }
}

@Composable
private fun TimelineTag(text: String, compact: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surfaceHigh)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = palette.fgMuted,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
