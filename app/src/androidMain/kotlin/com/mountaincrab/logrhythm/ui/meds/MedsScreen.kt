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
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.model.DoseStatus
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.TimeOfDay
import com.mountaincrab.logrhythm.data.model.daysFromMask
import com.mountaincrab.logrhythm.data.model.formatDose
import com.mountaincrab.logrhythm.data.model.formatMinutesOfDay
import com.mountaincrab.logrhythm.data.model.maskFromDays
import com.mountaincrab.logrhythm.ui.components.BottomTabBar
import com.mountaincrab.logrhythm.ui.components.FieldLabel
import com.mountaincrab.logrhythm.ui.navigation.Screen
import com.mountaincrab.logrhythm.ui.theme.AppPalette
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.util.formatTime
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.LocalDate

private enum class MedsTab(val label: String) { TODAY("Today"), SCHEDULE("Schedule"), CATALOG("Medications") }

@Composable
fun MedsScreen(
    onTabSelect: (route: String) -> Unit,
    onOpenEntry: (kind: String, id: String) -> Unit,
    viewModel: MedsViewModel = koinViewModel(),
) {
    val palette = LocalAppPalette.current
    var tab by remember { mutableStateOf(MedsTab.TODAY) }

    val today by viewModel.today.collectAsStateWithLifecycle()
    val scheduleRows by viewModel.scheduleRows.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val groupByMedication by viewModel.groupByMedication.collectAsStateWithLifecycle()

    var editingMedication by remember { mutableStateOf<MedicationEntity?>(null) }
    var showMedicationEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleRow?>(null) }
    var showScheduleEditor by remember { mutableStateOf(false) }

    if (showMedicationEditor) {
        MedicationEditorDialog(
            initial = editingMedication,
            onConfirm = { name, form, amount, unit ->
                viewModel.saveMedication(editingMedication?.id, name, form, amount, unit)
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
            onConfirm = { medicationId, amount, unit, minutes, rule, daysMask ->
                viewModel.saveSchedule(
                    id = editingSchedule?.schedule?.id,
                    medicationId = medicationId,
                    amount = amount,
                    unit = unit,
                    timeMinutes = minutes,
                    repeatRule = rule,
                    daysMask = daysMask,
                )
                showScheduleEditor = false
                editingSchedule = null
            },
            onCreateMedication = { name, form, amount, unit ->
                viewModel.saveMedication(null, name, form, amount, unit)
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
                text = todaySubtitle(today),
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
                MedsTab.TODAY -> todayTab(today, viewModel, onOpenEntry)
                MedsTab.SCHEDULE -> scheduleTab(
                    rows = scheduleRows,
                    grouped = groupByMedication,
                    onToggleGrouping = viewModel::toggleGrouping,
                    onEdit = { editingSchedule = it; showScheduleEditor = true },
                    onDelete = { viewModel.deleteSchedule(it) },
                    onToggleActive = { id, active -> viewModel.setScheduleActive(id, active) },
                    onAdd = { editingSchedule = null; showScheduleEditor = true },
                    hasMedications = medications.isNotEmpty(),
                )
                MedsTab.CATALOG -> catalogTab(
                    medications = medications,
                    onEdit = { editingMedication = it; showMedicationEditor = true },
                    onDelete = { viewModel.deleteMedication(it) },
                    onAdd = { editingMedication = null; showMedicationEditor = true },
                )
            }
        }

        BottomTabBar(active = Screen.Meds.route, onSelect = onTabSelect)
    }
}

private fun todaySubtitle(doses: List<TodayDose>): String {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM"))
    if (doses.isEmpty()) return "$date · nothing scheduled"
    val recorded = doses.count { it is TodayDose.Recorded }
    return "$date · $recorded of ${doses.size} doses so far"
}

// ── Today ──────────────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.todayTab(
    doses: List<TodayDose>,
    viewModel: MedsViewModel,
    onOpenEntry: (kind: String, id: String) -> Unit,
) {
    if (doses.isEmpty()) {
        item {
            EmptyCard(
                title = "No doses today",
                body = "Add a medication and put it on a schedule — doses then record themselves, " +
                    "and you only step in when something changes.",
            )
        }
        return
    }
    items(doses, key = { dose ->
        when (dose) {
            is TodayDose.Recorded -> "entry-${dose.entry.id}"
            is TodayDose.Upcoming -> "upcoming-${dose.dose.schedule.id}"
        }
    }) { dose ->
        when (dose) {
            is TodayDose.Recorded -> RecordedDoseCard(
                entry = dose.entry,
                onStatus = { status -> viewModel.setDoseStatus(dose.entry.id, status) },
                onAdjust = { amount, unit ->
                    viewModel.setDoseStatus(dose.entry.id, DoseStatus.ADJUSTED, amount, unit)
                },
                onOpen = { onOpenEntry("medicine", dose.entry.id) },
            )
            is TodayDose.Upcoming -> UpcomingDoseCard(
                dose = dose,
                onConfirm = { status -> viewModel.confirmUpcoming(dose.dose, status) },
            )
        }
    }
}

@Composable
private fun DoseStatus.color(palette: AppPalette): Color = when (this) {
    DoseStatus.SCHEDULED -> palette.fgMuted
    DoseStatus.TAKEN, DoseStatus.MANUAL -> palette.successText
    DoseStatus.ADJUSTED -> palette.warning
    DoseStatus.SKIPPED -> palette.dangerText
}

@Composable
private fun RecordedDoseCard(
    entry: MedicationEntryEntity,
    onStatus: (DoseStatus) -> Unit,
    onAdjust: (String, String) -> Unit,
    onOpen: () -> Unit,
) {
    val palette = LocalAppPalette.current
    var expanded by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }

    if (showAdjust) {
        AdjustDoseDialog(
            amount = entry.amount,
            unit = entry.unit,
            onConfirm = { a, u -> onAdjust(a, u); showAdjust = false; expanded = false },
            onDismiss = { showAdjust = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = entry.occurredAt.formatTime(),
                color = palette.fgMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.medicationName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
                val dose = formatDose(entry.amount, entry.unit)
                if (dose.isNotEmpty()) {
                    Text(text = dose, color = palette.fgMuted, fontSize = 12.5.sp)
                }
            }
            Text(
                text = entry.status.label,
                color = entry.status.color(palette),
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
            )
        }

        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton("Took it", Modifier.weight(1f), primary = true) { onStatus(DoseStatus.TAKEN) }
                ActionButton("Skipped", Modifier.weight(1f)) { onStatus(DoseStatus.SKIPPED) }
                ActionButton("Amount", Modifier.weight(1f)) { showAdjust = true }
            }
            ActionButton("Open entry", Modifier.fillMaxWidth(), onClick = onOpen)
        }
    }
}

@Composable
private fun UpcomingDoseCard(dose: TodayDose.Upcoming, onConfirm: (DoseStatus) -> Unit) {
    val palette = LocalAppPalette.current
    var expanded by remember { mutableStateOf(false) }
    val d = dose.dose

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.accentSoft)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = formatMinutesOfDay(d.schedule.timeMinutes),
                color = palette.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = d.medication.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
                val doseText = formatDose(d.schedule.amount, d.schedule.unit)
                Text(
                    text = if (doseText.isEmpty()) "Due later today" else "$doseText · due later today",
                    color = palette.fgMuted, fontSize = 12.5.sp,
                )
            }
            Text(text = "Due", color = palette.accentText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton("Took it now", Modifier.weight(1f), primary = true) { onConfirm(DoseStatus.TAKEN) }
                ActionButton("Skip", Modifier.weight(1f)) { onConfirm(DoseStatus.SKIPPED) }
            }
        }
    }
}

@Composable
private fun AdjustDoseDialog(
    amount: String,
    unit: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var a by remember { mutableStateOf(amount) }
    var u by remember { mutableStateOf(unit) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust amount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Record what you actually took — the dose stays on the timeline, marked as adjusted.",
                    fontSize = 13.sp,
                    color = LocalAppPalette.current.fgMuted,
                )
                DoseFields(a, u, { a = it }, { u = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(a, u) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Schedule ───────────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.scheduleTab(
    rows: List<ScheduleRow>,
    grouped: Boolean,
    onToggleGrouping: () -> Unit,
    onEdit: (ScheduleRow) -> Unit,
    onDelete: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onAdd: () -> Unit,
    hasMedications: Boolean,
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
                    "Add a dose and it'll record itself each day — you only touch it when you miss one."
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
                    onDelete = { onDelete(row.schedule.id) },
                    onToggleActive = { onToggleActive(row.schedule.id, !row.schedule.isActive) })
            }
        }
    } else {
        items(rows, key = { "flat-${it.schedule.id}" }) { row ->
            ScheduleCard(row, showName = true, onEdit = { onEdit(row) },
                onDelete = { onDelete(row.schedule.id) },
                onToggleActive = { onToggleActive(row.schedule.id, !row.schedule.isActive) })
        }
    }

    if (hasMedications) {
        item { AddRowButton("+ Add a dose", onAdd) }
    }
}

@Composable
private fun ScheduleCard(
    row: ScheduleRow,
    showName: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
                val dose = formatDose(s.amount, s.unit)
                Text(
                    text = listOfNotNull(dose.takeIf { it.isNotEmpty() }, repeatLabel(s.repeatRule, s.daysMask))
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
            ActionButton("Delete", Modifier.weight(1f), danger = true, onClick = onDelete)
        }
        if (!s.isActive) {
            Text("Paused — not recording doses", color = palette.warning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
    onConfirm: (medicationId: String, amount: String, unit: String, minutes: Int, rule: RepeatRule, daysMask: Int) -> Unit,
    onCreateMedication: (String, com.mountaincrab.logrhythm.data.model.MedicationForm, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var medicationId by remember { mutableStateOf(initial?.schedule?.medicationId ?: medications.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(initial?.schedule?.amount ?: medications.firstOrNull()?.defaultAmount.orEmpty()) }
    var unit by remember { mutableStateOf(initial?.schedule?.unit ?: medications.firstOrNull()?.defaultUnit.orEmpty()) }
    var minutes by remember { mutableIntStateOf(initial?.schedule?.timeMinutes ?: TimeOfDay.MORNING.defaultMinutes) }
    var rule by remember { mutableStateOf(initial?.schedule?.repeatRule ?: RepeatRule.DAILY) }
    var days by remember { mutableStateOf(daysFromMask(initial?.schedule?.daysMask ?: 0).toSet()) }
    var showNewMedication by remember { mutableStateOf(false) }

    if (showNewMedication) {
        MedicationEditorDialog(
            initial = null,
            onConfirm = { name, form, a, u ->
                onCreateMedication(name, form, a, u)
                showNewMedication = false
            },
            onDismiss = { showNewMedication = false },
        )
    }

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
                        onSelect = { med ->
                            medicationId = med.id
                            amount = med.defaultAmount
                            unit = med.defaultUnit
                        },
                        onCreateNew = { showNewMedication = true },
                    )
                }
                Column {
                    FieldLabel("Dose")
                    DoseFields(amount, unit, { amount = it }, { unit = it })
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
                    medicationId?.let { onConfirm(it, amount, unit, minutes, rule, maskFromDays(days)) }
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
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
) {
    if (medications.isEmpty()) {
        item {
            EmptyCard(
                title = "No medications yet",
                body = "Define each medication once here, then pick it when scheduling a dose or logging one by hand.",
            )
        }
    }
    items(medications, key = { it.id }) { med ->
        MedicationCard(med, onEdit = { onEdit(med) }, onDelete = { onDelete(med.id) })
    }
    item { AddRowButton("+ Add medication", onAdd) }
}

@Composable
private fun MedicationCard(medication: MedicationEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalAppPalette.current
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${medication.name}?") },
            text = {
                Text(
                    "Its scheduled doses stop, but doses already recorded stay on your timeline.",
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
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
        Text(text = medication.form.emoji(), fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = medication.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
            val dose = formatDose(medication.defaultAmount, medication.defaultUnit)
            Text(
                text = listOfNotNull(medication.form.label, dose.takeIf { it.isNotEmpty() }).joinToString(" · "),
                color = palette.fgMuted, fontSize = 12.5.sp,
            )
        }
        Icon(
            Icons.Outlined.Edit, contentDescription = "Edit",
            tint = palette.fgMuted,
            modifier = Modifier.size(18.dp).clickable(onClick = onEdit),
        )
        Icon(
            Icons.Outlined.DeleteOutline, contentDescription = "Delete",
            tint = palette.dangerText,
            modifier = Modifier.size(18.dp).clickable { confirmDelete = true },
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
