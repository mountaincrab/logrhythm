@file:OptIn(ExperimentalLayoutApi::class)

package com.mountaincrab.logrhythm.ui.meds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.dose
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.TimeOfDay
import com.mountaincrab.logrhythm.data.model.daysFromMask
import com.mountaincrab.logrhythm.data.model.formatDoseAmount
import com.mountaincrab.logrhythm.data.model.formatMinutesOfDay
import com.mountaincrab.logrhythm.data.model.maskFromDays
import com.mountaincrab.logrhythm.ui.components.MedicationFormIcon
import com.mountaincrab.logrhythm.ui.components.BottomTabBar
import com.mountaincrab.logrhythm.ui.components.FieldLabel
import com.mountaincrab.logrhythm.ui.navigation.Screen
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import org.koin.compose.viewmodel.koinViewModel

private enum class MedsTab(val label: String) { SCHEDULE("Schedule"), CATALOG("Medications") }

/**
 * Meds is where medication is *set up* — the medications you take, and the schedules that
 * add dose entries for you. Doses themselves live on the timeline with every other entry;
 * there is deliberately no second place to review or confirm them.
 */
@Composable
fun MedsScreen(
    onTabSelect: (route: String) -> Unit,
    viewModel: MedsViewModel = koinViewModel(),
) {
    val palette = LocalAppPalette.current
    var tab by remember { mutableStateOf(MedsTab.SCHEDULE) }

    val scheduleRows by viewModel.scheduleRows.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val archivedMedications by viewModel.archivedMedications.collectAsStateWithLifecycle()
    val archivedScheduleRows by viewModel.archivedScheduleRows.collectAsStateWithLifecycle()
    val groupByMedication by viewModel.groupByMedication.collectAsStateWithLifecycle()

    var editingMedication by remember { mutableStateOf<MedicationEntity?>(null) }
    var showMedicationEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleRow?>(null) }
    var showScheduleEditor by remember { mutableStateOf(false) }

    if (showMedicationEditor) {
        MedicationEditorDialog(
            initial = editingMedication,
            onConfirm = { name, form, doseAmount, doseUnit ->
                viewModel.saveMedication(editingMedication?.id, name, form, doseAmount, doseUnit)
                showMedicationEditor = false
                editingMedication = null
            },
            onDismiss = { showMedicationEditor = false; editingMedication = null },
        )
    }

    if (showScheduleEditor) {
        ScheduleEditorDialog(
            initial = editingSchedule,
            medications = medications,
            onConfirm = { medicationId, quantity, minutes, rule, daysMask ->
                viewModel.saveSchedule(
                    id = editingSchedule?.schedule?.id,
                    medicationId = medicationId,
                    quantity = quantity,
                    timeMinutes = minutes,
                    repeatRule = rule,
                    daysMask = daysMask,
                )
                showScheduleEditor = false
                editingSchedule = null
            },
            onCreateMedication = { name, form, doseAmount, doseUnit ->
                viewModel.saveMedication(null, name, form, doseAmount, doseUnit)
            },
            onDismiss = { showScheduleEditor = false; editingSchedule = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
        ) {
            Text(
                text = "Meds",
                fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Doses are added to your timeline automatically",
                fontSize = 13.sp, color = palette.fgMuted,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MedsTab.entries.forEach { t ->
                SelectChip(
                    text = t.label,
                    selected = t == tab,
                    modifier = Modifier.weight(1f),
                    onClick = { tab = t },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (tab) {
                MedsTab.SCHEDULE -> scheduleTab(
                    rows = scheduleRows,
                    grouped = groupByMedication,
                    onToggleGrouping = viewModel::toggleGrouping,
                    onEdit = { editingSchedule = it; showScheduleEditor = true },
                    onArchive = { viewModel.archiveSchedule(it) },
                    onToggleActive = { id, active -> viewModel.setScheduleActive(id, active) },
                    onAdd = { editingSchedule = null; showScheduleEditor = true },
                    hasMedications = medications.isNotEmpty(),
                    archivedRows = archivedScheduleRows,
                    onRestore = { viewModel.restoreSchedule(it) },
                )
                MedsTab.CATALOG -> catalogTab(
                    medications = medications,
                    onEdit = { editingMedication = it; showMedicationEditor = true },
                    onArchive = { viewModel.archiveMedication(it) },
                    onAdd = { editingMedication = null; showMedicationEditor = true },
                    archived = archivedMedications,
                    onRestore = { viewModel.restoreMedication(it) },
                )
            }
        }

        BottomTabBar(active = Screen.Meds.route, onSelect = onTabSelect)
    }
}

// ── Schedule ───────────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.scheduleTab(
    rows: List<ScheduleRow>,
    grouped: Boolean,
    onToggleGrouping: () -> Unit,
    onEdit: (ScheduleRow) -> Unit,
    onArchive: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onAdd: () -> Unit,
    hasMedications: Boolean,
    archivedRows: List<ScheduleRow>,
    onRestore: (String) -> Unit,
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (rows.isEmpty()) "No scheduled doses" else "${rows.size} scheduled dose${if (rows.size == 1) "" else "s"}",
                modifier = Modifier.weight(1f),
                color = LocalAppPalette.current.fgMuted,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            )
            SelectChip(
                text = if (grouped) "By medication" else "By dose",
                selected = grouped,
                onClick = onToggleGrouping,
            )
        }
    }

    if (rows.isEmpty()) {
        item {
            EmptyCard(
                title = "Nothing scheduled",
                body = if (hasMedications) {
                    "Add a dose and it'll be logged for you each day. Miss one? Delete it from your timeline."
                } else {
                    "Add a medication first, then schedule the doses you take."
                },
            )
        }
    } else if (grouped) {
        // Same rows, grouped by the medication each one points at.
        val groups = rows.groupBy { it.schedule.medicationId }.values.sortedBy { it.first().name }
        groups.forEach { group ->
            item(key = "group-${group.first().schedule.medicationId}") {
                Text(
                    text = group.first().name,
                    color = LocalAppPalette.current.fgMuted,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            items(group, key = { "grouped-${it.schedule.id}" }) { row ->
                ScheduleCard(row, showName = false, onEdit = { onEdit(row) },
                    onArchive = { onArchive(row.schedule.id) },
                    onToggleActive = { onToggleActive(row.schedule.id, !row.schedule.isActive) })
            }
        }
    } else {
        items(rows, key = { "flat-${it.schedule.id}" }) { row ->
            ScheduleCard(row, showName = true, onEdit = { onEdit(row) },
                onArchive = { onArchive(row.schedule.id) },
                onToggleActive = { onToggleActive(row.schedule.id, !row.schedule.isActive) })
        }
    }

    if (hasMedications) {
        item { AddRowButton("+ Add a dose", onAdd) }
    }

    if (archivedRows.isNotEmpty()) {
        item { ArchivedHeader("Restored doses start from today — nothing is back-filled.") }
        items(archivedRows, key = { "archived-${it.schedule.id}" }) { row ->
            ArchivedRow(
                title = row.name,
                subtitle = listOfNotNull(
                    formatMinutesOfDay(row.schedule.timeMinutes),
                    repeatLabel(row.schedule.repeatRule, row.schedule.daysMask),
                ).joinToString(" · "),
                onRestore = { onRestore(row.schedule.id) },
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    row: ScheduleRow,
    showName: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onToggleActive: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val s = row.schedule
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                if (showName) {
                    Text(
                        text = row.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    )
                }
                val amount = formatDoseAmount(s.quantity, row.dose)
                Text(
                    text = listOfNotNull(amount.takeIf { it.isNotEmpty() }, repeatLabel(s.repeatRule, s.daysMask))
                        .joinToString(" · "),
                    color = palette.fgMuted, fontSize = 12.5.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMinutesOfDay(s.timeMinutes),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    text = TimeOfDay.forMinutes(s.timeMinutes).label,
                    color = palette.fgFaint, fontSize = 11.sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(if (s.isActive) "Pause" else "Resume", Modifier.weight(1f), onClick = onToggleActive)
            ActionButton("Edit", Modifier.weight(1f), onClick = onEdit)
            ActionButton("Archive", Modifier.weight(1f), danger = true, onClick = onArchive)
        }
        if (!s.isActive) {
            Text("Paused — not adding doses", color = palette.warning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun repeatLabel(rule: RepeatRule, daysMask: Int): String = when (rule) {
    RepeatRule.SPECIFIC_DAYS -> {
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val days = daysFromMask(daysMask).map { names[it - 1] }
        if (days.isEmpty()) "No days picked" else days.joinToString(" · ")
    }
    else -> rule.label
}

@Composable
private fun ScheduleEditorDialog(
    initial: ScheduleRow?,
    medications: List<MedicationEntity>,
    onConfirm: (medicationId: String, quantity: String, minutes: Int, rule: RepeatRule, daysMask: Int) -> Unit,
    onCreateMedication: (String, com.mountaincrab.logrhythm.data.model.MedicationForm, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var medicationId by remember { mutableStateOf(initial?.schedule?.medicationId ?: medications.firstOrNull()?.id) }
    var quantity by remember { mutableStateOf(initial?.schedule?.quantity ?: "1") }
    var minutes by remember { mutableIntStateOf(initial?.schedule?.timeMinutes ?: TimeOfDay.MORNING.defaultMinutes) }
    var rule by remember { mutableStateOf(initial?.schedule?.repeatRule ?: RepeatRule.DAILY) }
    var days by remember { mutableStateOf(daysFromMask(initial?.schedule?.daysMask ?: 0).toSet()) }
    var showNewMedication by remember { mutableStateOf(false) }

    if (showNewMedication) {
        MedicationEditorDialog(
            initial = null,
            onConfirm = { name, form, doseAmount, doseUnit ->
                onCreateMedication(name, form, doseAmount, doseUnit)
                showNewMedication = false
            },
            onDismiss = { showNewMedication = false },
        )
    }

    val selected = medications.firstOrNull { it.id == medicationId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add a dose" else "Edit dose") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column {
                    FieldLabel("Medication")
                    MedicationPicker(
                        medications = medications,
                        selectedId = medicationId,
                        onSelect = { med -> medicationId = med.id },
                        onCreateNew = { showNewMedication = true },
                    )
                }
                Column {
                    // The strength lives on the medication, so a schedule only says how many.
                    FieldLabel("Quantity", hint = selected?.dose?.takeIf { it.isNotBlank() }?.let { "× $it" })
                    PlainInput(quantity, { quantity = it }, "e.g. 2", modifier = Modifier.fillMaxWidth())
                }
                Column {
                    FieldLabel("When")
                    TimeOfDayPicker(selectedMinutes = minutes, onSelect = { minutes = it })
                    Spacer(Modifier.height(8.dp))
                    MinutesPickerField(minutes = minutes, onChange = { minutes = it })
                }
                Column {
                    FieldLabel("Repeats")
                    RepeatChips(selected = rule, onSelect = { rule = it })
                    if (rule == RepeatRule.SPECIFIC_DAYS) {
                        Spacer(Modifier.height(8.dp))
                        DayOfWeekPicker(
                            selectedDays = days,
                            onToggle = { iso -> days = if (iso in days) days - iso else days + iso },
                        )
                    }
                }
                Text(
                    text = "This adds one scheduled dose. Taking the same medication morning and " +
                        "night? Save this, then add a second dose.",
                    color = LocalAppPalette.current.fgMuted,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = medicationId != null && (rule != RepeatRule.SPECIFIC_DAYS || days.isNotEmpty()),
                onClick = {
                    medicationId?.let { onConfirm(it, quantity, minutes, rule, maskFromDays(days)) }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Catalog ────────────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.catalogTab(
    medications: List<MedicationEntity>,
    onEdit: (MedicationEntity) -> Unit,
    onArchive: (String) -> Unit,
    onAdd: () -> Unit,
    archived: List<MedicationEntity>,
    onRestore: (String) -> Unit,
) {
    if (medications.isEmpty()) {
        item {
            EmptyCard(
                title = "No medications yet",
                body = "Define each medication once here — name, form and strength, e.g. " +
                    "\"Pentasa, tablet, 1g\" — then pick it when scheduling a dose or logging one by hand.",
            )
        }
    }
    items(medications, key = { it.id }) { med ->
        MedicationCard(med, onEdit = { onEdit(med) }, onArchive = { onArchive(med.id) })
    }
    item { AddRowButton("+ Add medication", onAdd) }

    if (archived.isNotEmpty()) {
        item { ArchivedHeader("Doses you already recorded still read from these.") }
        items(archived, key = { "archived-${it.id}" }) { med ->
            ArchivedRow(
                title = med.name,
                subtitle = listOfNotNull(med.form.label, med.dose.takeIf { it.isNotBlank() })
                    .joinToString(" · "),
                onRestore = { onRestore(med.id) },
            )
        }
    }
}

/**
 * Archived medications and schedules are hidden from the pickers, never removed: recorded
 * doses read their name and strength through them.
 */
@Composable
private fun ArchivedHeader(body: String) {
    val palette = LocalAppPalette.current
    Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "ARCHIVED",
            color = palette.fgMuted,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp,
        )
        Text(text = body, color = palette.fgFaint, fontSize = 12.sp)
    }
}

@Composable
private fun ArchivedRow(title: String, subtitle: String, onRestore: () -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = palette.fgMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, color = palette.fgFaint, fontSize = 12.sp)
            }
        }
        ActionButton("Restore", onClick = onRestore)
    }
}

@Composable
private fun MedicationCard(medication: MedicationEntity, onEdit: () -> Unit, onArchive: () -> Unit) {
    val palette = LocalAppPalette.current
    var confirmArchive by remember { mutableStateOf(false) }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archive ${medication.name}?") },
            text = {
                Text(
                    "Its scheduled doses stop and it leaves the pickers. Doses you already " +
                        "recorded stay on your timeline and keep reading their name and strength " +
                        "from it. You can restore it from the bottom of this tab.",
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { onArchive(); confirmArchive = false }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MedicationFormIcon(form = medication.form, size = 24.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = medication.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                text = listOfNotNull(medication.form.label, medication.dose.takeIf { it.isNotBlank() })
                    .joinToString(" · "),
                color = palette.fgMuted, fontSize = 12.5.sp,
            )
        }
        Icon(
            Icons.Outlined.Edit, contentDescription = "Edit",
            tint = palette.fgMuted,
            modifier = Modifier.size(18.dp).clickable(onClick = onEdit),
        )
        Icon(
            Icons.Outlined.Archive, contentDescription = "Archive",
            tint = palette.dangerText,
            modifier = Modifier.size(18.dp).clickable { confirmArchive = true },
        )
    }
}

// ── shared bits ────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (primary) MaterialTheme.colorScheme.primary else palette.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = when {
                primary -> MaterialTheme.colorScheme.onPrimary
                danger -> palette.dangerText
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AddRowButton(text: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, palette.borderStrong, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = palette.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = body, color = palette.fgMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
