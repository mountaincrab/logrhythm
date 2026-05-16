package com.mountaincrab.logrhythm.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
import com.mountaincrab.logrhythm.ui.components.BottomTabBar
import com.mountaincrab.logrhythm.ui.components.TimelineEntryRow
import com.mountaincrab.logrhythm.ui.navigation.Screen
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.util.formatDayLabel
import com.mountaincrab.logrhythm.ui.util.startOfDayMillis
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@Composable
fun HomeScreen(
    onOpenAddPoop: () -> Unit,
    onOpenAddFood: () -> Unit,
    onOpenAddNote: () -> Unit,
    onOpenEntry: (kind: String, id: String) -> Unit,
    onTabSelect: (route: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Home",
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = MaterialTheme.colorScheme.onBackground)
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM"))
                val summary = buildString {
                    append(today)
                    append(" · ${state.todayPoopCount} ")
                    append(if (state.todayPoopCount == 1) "poop" else "poops")
                    state.todayWorstRating?.let { append(" · rating $it") }
                }
                Text(text = summary, fontSize = 13.sp, color = palette.fgMuted)
            }
        }

        // Timeline
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            if (state.days.isEmpty()) {
                item { EmptyState() }
            }
            state.days.forEach { day ->
                item(key = "header-${day.date}") {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = startOfDayMillis(day.date).formatDayLabel(),
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = palette.fgMuted,
                        )
                        Text(
                            text = "${day.entries.size} ${if (day.entries.size == 1) "entry" else "entries"}",
                            fontSize = 11.sp, color = palette.fgFaint, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                items(day.entries, key = { it.id }) { entry ->
                    TimelineEntryRow(
                        entry = entry,
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                            .drawTimelineLine(palette.border),
                        onClick = { onOpenEntry(entry.kindKey(), entry.id) },
                    )
                }
            }
        }

        // Bottom log bar — 3 quick log buttons.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, palette.border)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogButton(emoji = "💩", label = "Poop", modifier = Modifier.weight(1f), onClick = onOpenAddPoop)
            LogButton(emoji = "🍴", label = "Food", modifier = Modifier.weight(1f), onClick = onOpenAddFood)
            LogButton(emoji = "📝", label = "Note", modifier = Modifier.weight(1f), onClick = onOpenAddNote)
        }

        BottomTabBar(active = Screen.Home.route, onSelect = onTabSelect)
    }
}

private fun TimelineEntry.kindKey(): String = when (this) {
    is TimelineEntry.Poop -> "poop"
    is TimelineEntry.Food -> "food"
    is TimelineEntry.Note -> "note"
}

@Composable
private fun LogButton(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Text(text = label, color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyState() {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No entries yet", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = "Tap one of the buttons below to log a poop, food, or note.",
            color = palette.fgMuted, fontSize = 13.sp,
        )
    }
}

private fun Modifier.drawTimelineLine(color: androidx.compose.ui.graphics.Color): Modifier = drawBehind {
    val x = 7.dp.toPx()
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.dp.toPx(),
    )
}
