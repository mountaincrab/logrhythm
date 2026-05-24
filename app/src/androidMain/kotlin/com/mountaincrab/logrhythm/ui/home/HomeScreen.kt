package com.mountaincrab.logrhythm.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
import com.mountaincrab.logrhythm.ui.components.BottomTabBar
import com.mountaincrab.logrhythm.ui.components.TimelineEntryRow
import com.mountaincrab.logrhythm.ui.navigation.Screen
import com.mountaincrab.logrhythm.ui.profiles.ProfileAvatar
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.util.formatDayLabel
import com.mountaincrab.logrhythm.ui.util.startOfDayMillis
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
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
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    var showProfileSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showProfileSheet) {
        ProfileSwitcherSheet(
            profiles = profiles,
            activeId = activeProfile?.id,
            onSelect = { viewModel.selectProfile(it); showProfileSheet = false },
            onAdd = { showProfileSheet = false; showAddDialog = true },
            onDismiss = { showProfileSheet = false },
        )
    }
    if (showAddDialog) {
        AddProfileDialog(
            onConfirm = { viewModel.addProfile(it); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }

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
            Box(modifier = Modifier.clip(CircleShape).clickable { showProfileSheet = true }) {
                ProfileAvatar(name = activeProfile?.name ?: "?", highlighted = true, size = 38)
            }
        }

        // Timeline with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.sync() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSwitcherSheet(
    profiles: List<ProfileEntity>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                "Profiles",
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            profiles.forEach { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(profile.id) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileAvatar(name = profile.name, highlighted = profile.id == activeId)
                    Text(
                        profile.name,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                    if (profile.id == activeId) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAdd)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.surfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    "Add profile",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AddProfileDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New profile") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Alex") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
