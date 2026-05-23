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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountaincrab.logrhythm.data.model.bristol
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val dotColor = when (entry) {
        is TimelineEntry.Poop -> RatingColors[entry.entity.blood]?.bg ?: palette.surfaceHigh
        is TimelineEntry.Food -> palette.surfaceHigh
        is TimelineEntry.Note -> palette.warning
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // dot — positioned by parent's timeline padding (22dp from left).
        Box(
            modifier = Modifier
                .padding(start = 2.dp, top = 16.dp)
                .size(11.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
        )
        Column(
            modifier = Modifier
                .padding(start = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceRaised)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (entry) {
                is TimelineEntry.Poop -> PoopBody(entry)
                is TimelineEntry.Food -> FoodBody(entry)
                is TimelineEntry.Note -> NoteBody(entry)
            }
        }
    }
}

@Composable
private fun PoopBody(entry: TimelineEntry.Poop) {
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = entry.entity.occurredAt.formatTime(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = "💩", fontSize = 14.sp)
        if (bristolText.isNotEmpty()) {
            Text(text = bristolText, color = palette.fgMuted, fontSize = 13.sp)
        }
        RatingPill(rating = entry.entity.blood)
        entry.tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.surfaceHigh)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(tag.name, color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (!entry.entity.notes.isNullOrBlank()) {
        Text(text = entry.entity.notes!!, color = palette.fgMuted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun FoodBody(entry: TimelineEntry.Food) {
    val palette = LocalAppPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = entry.entity.occurredAt.formatTime(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = "🍴", fontSize = 14.sp)
    }
    Text(text = entry.entity.items, color = palette.fgMuted, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun NoteBody(entry: TimelineEntry.Note) {
    val palette = LocalAppPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = entry.entity.occurredAt.formatTime(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = "📝", fontSize = 14.sp)
        if (entry.entity.caffeine) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.surfaceHigh)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("☕", color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (entry.entity.alcohol) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.surfaceHigh)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("🍺", color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        entry.tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.surfaceHigh)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(tag.name, color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (entry.entity.content.isNotBlank()) {
        Text(text = entry.entity.content, color = palette.fgMuted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
