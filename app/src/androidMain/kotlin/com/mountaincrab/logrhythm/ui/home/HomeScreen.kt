package com.mountaincrab.logrhythm.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
import com.mountaincrab.logrhythm.preferences.HomeTimelineDensity
import com.mountaincrab.logrhythm.ui.components.BottomTabBar
import com.mountaincrab.logrhythm.ui.components.EntryIconSizes
import com.mountaincrab.logrhythm.ui.components.MedicineIcon
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
    onOpenAddMedicine: () -> Unit,
    onOpenEntry: (kind: String, id: String) -> Unit,
    onTabSelect: (route: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val homeTimelineDensity by viewModel.homeTimelineDensity.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current
    val compactTimeline = homeTimelineDensity == HomeTimelineDensity.COMPACT

    var showProfileSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    // When a new entry becomes the top-most item (e.g. after saving the first
    // poop of a new day), scroll the timeline back to the top so it's visible.
    // LazyColumn otherwise keeps the scroll anchored to the previously-visible
    // items by key, leaving the list stuck on an older day.
    val topEntryId = state.days.firstOrNull()?.entries?.firstOrNull()?.id
    LaunchedEffect(topEntryId) {
        if (topEntryId != null) {
            listState.animateScrollToItem(0)
        }
    }

    // Load the next page when the user scrolls within a few items of the end.
    val reachedEnd by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            layout.totalItemsCount > 0 && last >= layout.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd, state.hasMore, state.loadingMore, state.enabledEntryTypes) {
        if (reachedEnd && state.hasMore && !state.loadingMore && state.enabledEntryTypes.isNotEmpty()) {
            viewModel.loadMore()
        }
    }

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


        EntryFilterBar(
            enabledTypes = state.enabledEntryTypes,
            onToggle = viewModel::toggleEntryType,
            onClear = viewModel::clearEntryFilters,
        )

        // Timeline with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.sync() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                if (state.loading) {
                    item { LoadingIndicator() }
                } else if (state.totalEntryCount == 0) {
                    item { EmptyState() }
                } else if (state.days.isEmpty()) {
                    item { FilteredEmptyState(onClear = viewModel::clearEntryFilters) }
                }
                state.days.forEach { day ->
                    item(key = "header-${day.date}") {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = if (compactTimeline) 10.dp else 14.dp,
                                    bottom = if (compactTimeline) 6.dp else 10.dp,
                                ),
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
                                text = if (state.filtersActive) {
                                    "${day.entries.size} of ${day.totalEntryCount}"
                                } else {
                                    "${day.entries.size} ${if (day.entries.size == 1) "entry" else "entries"}"
                                },
                                fontSize = 11.sp, color = palette.fgFaint, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    items(day.entries, key = { it.id }) { entry ->
                        TimelineEntryRow(
                            entry = entry,
                            density = homeTimelineDensity,
                            modifier = Modifier
                                .padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    bottom = if (compactTimeline) 4.dp else 8.dp,
                                )
                                .drawTimelineLine(palette.border),
                            onClick = { onOpenEntry(entry.kindKey(), entry.id) },
                        )
                    }
                }
                if (state.loadingMore) {
                    item(key = "paging-footer") { LoadingIndicator() }
                }
            }
        }

        // Bottom log bar — quick log buttons. Medicine covers one-off doses; anything on
        // a schedule records itself and is corrected from the Meds tab.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, palette.border)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogButton(label = "Poop", modifier = Modifier.weight(1f), onClick = onOpenAddPoop) {
                Text(text = "💩", fontSize = EntryIconSizes.ButtonEmoji)
            }
            LogButton(label = "Food", modifier = Modifier.weight(1f), onClick = onOpenAddFood) {
                Text(text = "🍴", fontSize = EntryIconSizes.ButtonEmoji)
            }
            LogButton(label = "Note", modifier = Modifier.weight(1f), onClick = onOpenAddNote) {
                Text(text = "📝", fontSize = EntryIconSizes.ButtonEmoji)
            }
            LogButton(label = "Medicine", modifier = Modifier.weight(1f), onClick = onOpenAddMedicine) {
                MedicineIcon(size = EntryIconSizes.ButtonIcon)
            }
        }

        BottomTabBar(active = Screen.Home.route, onSelect = onTabSelect)
    }
}

private data class EntryFilterOption(
    val type: HomeEntryType,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
private fun EntryFilterBar(
    enabledTypes: Set<HomeEntryType>,
    onToggle: (HomeEntryType) -> Unit,
    onClear: () -> Unit,
) {
    val options = listOf(
        EntryFilterOption(HomeEntryType.POOP, "Poop") {
            Text(text = "💩", fontSize = 16.sp, lineHeight = 16.sp)
        },
        EntryFilterOption(HomeEntryType.FOOD, "Food") {
            Text(text = "🍴", fontSize = 16.sp, lineHeight = 16.sp)
        },
        EntryFilterOption(HomeEntryType.NOTE, "Note") {
            Text(text = "📝", fontSize = 16.sp, lineHeight = 16.sp)
        },
        EntryFilterOption(HomeEntryType.MEDICINE, "Medicine") {
            MedicineIcon(size = EntryIconSizes.ChipIcon)
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            EntryFilterChip(
                label = option.label,
                selected = option.type in enabledTypes,
                onToggle = { onToggle(option.type) },
                icon = option.icon,
            )
        }
        if (enabledTypes.size != HomeEntryType.entries.size) {
            ClearFilterChip(onClick = onClear)
        }
    }
}

@Composable
private fun EntryFilterChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) palette.accentSoft else palette.surfaceRaised)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else palette.border,
                shape = shape,
            )
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .alpha(if (selected) 1f else 0.35f),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = label,
            color = if (selected) palette.accentText else palette.fgFaint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ClearFilterChip(onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val borderColor = palette.borderStrong
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            tint = palette.fgMuted,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = "Clear",
            color = palette.fgMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun TimelineEntry.kindKey(): String = when (this) {
    is TimelineEntry.Poop -> "poop"
    is TimelineEntry.Food -> "food"
    is TimelineEntry.Note -> "note"
    is TimelineEntry.Medication -> "medicine"
}

@Composable
private fun LogButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
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
        // A fixed slot, sized for the tallest mark: the drawn bottle needs more box than
        // the emoji do, and without this the Medicine card stood taller than its three
        // neighbours and its label sat a couple of dp lower.
        Box(
            modifier = Modifier.height(EntryIconSizes.ButtonIcon),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Text(text = label, color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = LocalAppPalette.current.fgMuted,
        )
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
            text = "Tap one of the buttons below to log a poop, food, note, or dose.",
            color = palette.fgMuted, fontSize = 13.sp,
        )
    }
}

@Composable
private fun FilteredEmptyState(onClear: () -> Unit) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "No matching entries",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Turn an entry type back on or clear the filter.",
            color = palette.fgMuted,
            fontSize = 13.sp,
        )
        TextButton(onClick = onClear) {
            Text("Clear filter")
        }
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
