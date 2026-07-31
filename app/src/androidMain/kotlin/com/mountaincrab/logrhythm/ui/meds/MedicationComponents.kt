@file:OptIn(ExperimentalLayoutApi::class)

package com.mountaincrab.logrhythm.ui.meds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.TimeOfDay
import com.mountaincrab.logrhythm.data.model.formatDose
import com.mountaincrab.logrhythm.data.model.formatMinutesOfDay
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette

/** Emoji stand-in for the design's pill / spray-can icons. */
fun MedicationForm.emoji(): String = if (isTopical) "🧴" else "💊"

/** A pill-shaped selectable chip — the shared look for forms, repeats and medications. */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) palette.accentSoft else palette.surfaceRaised)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else palette.border,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (selected) palette.accentText else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Bordered single-line text input matching the design's `.inp`. */
@Composable
fun PlainInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = palette.fgFaint, fontSize = 15.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Amount + unit side by side, e.g. "2" / "g". */
@Composable
fun DoseFields(
    amount: String,
    unit: String,
    onAmountChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PlainInput(amount, onAmountChange, "2", modifier = Modifier.weight(1f))
        PlainInput(unit, onUnitChange, "g", modifier = Modifier.weight(1f))
    }
}

@Composable
fun MedicationFormChips(selected: MedicationForm, onSelect: (MedicationForm) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MedicationForm.entries.forEach { form ->
            SelectChip(text = form.label, selected = form == selected, onClick = { onSelect(form) })
        }
    }
}

@Composable
fun RepeatChips(selected: RepeatRule, onSelect: (RepeatRule) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RepeatRule.entries.forEach { rule ->
            SelectChip(text = rule.label, selected = rule == selected, onClick = { onSelect(rule) })
        }
    }
}

/** Mon–Sun toggles, shown only for [RepeatRule.SPECIFIC_DAYS]. */
@Composable
fun DayOfWeekPicker(selectedDays: Set<Int>, onToggle: (Int) -> Unit) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val iso = index + 1
            val on = iso in selectedDays
            val palette = LocalAppPalette.current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) palette.accentSoft else palette.surfaceRaised)
                    .border(
                        1.dp,
                        if (on) MaterialTheme.colorScheme.primary else palette.border,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onToggle(iso) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (on) palette.accentText else palette.fgMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** The four time-of-day buckets from the design, each seeding a sensible default time. */
@Composable
fun TimeOfDayPicker(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    val palette = LocalAppPalette.current
    val active = TimeOfDay.forMinutes(selectedMinutes)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        TimeOfDay.entries.forEach { tod ->
            val on = tod == active
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) palette.accentSoft else palette.surfaceRaised)
                    .border(
                        1.dp,
                        if (on) MaterialTheme.colorScheme.primary else palette.border,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(tod.defaultMinutes) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = tod.label,
                    color = if (on) palette.accentText else palette.fgMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Shows the dose's exact time and opens a clock to change it. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MinutesPickerField(minutes: Int, onChange: (Int) -> Unit) {
    val palette = LocalAppPalette.current
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Text(
            text = formatMinutesOfDay(minutes),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }

    if (showPicker) {
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = minutes / 60,
            initialMinute = minutes % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Dose time") },
            text = { androidx.compose.material3.TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.hour * 60 + state.minute)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        )
    }
}

/**
 * Picks a medication out of the catalog. Creating one inline is deliberate — a medication
 * is defined once here and then referenced by both manual doses and scheduled ones.
 */
@Composable
fun MedicationPicker(
    medications: List<MedicationEntity>,
    selectedId: String?,
    onSelect: (MedicationEntity) -> Unit,
    onCreateNew: () -> Unit,
) {
    val palette = LocalAppPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        medications.forEach { med ->
            val label = listOf(med.form.emoji() + " " + med.name, formatDose(med.defaultAmount, med.defaultUnit))
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            SelectChip(text = label, selected = med.id == selectedId, onClick = { onSelect(med) })
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surfaceRaised)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                .clickable(onClick = onCreateNew)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = "+ New medication", color = palette.fgMuted, fontSize = 13.sp)
        }
    }
}

/** Create/edit a catalog medication: name, form and the dose it's usually taken at. */
@Composable
fun MedicationEditorDialog(
    initial: MedicationEntity?,
    onConfirm: (name: String, form: MedicationForm, amount: String, unit: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var form by remember { mutableStateOf(initial?.form ?: MedicationForm.TABLET) }
    var amount by remember { mutableStateOf(initial?.defaultAmount.orEmpty()) }
    var unit by remember { mutableStateOf(initial?.defaultUnit.orEmpty()) }
    val palette = LocalAppPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New medication" else "Edit medication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Mesalazine (Octasa)") },
                    singleLine = true,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("FORM", color = palette.fgMuted, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    MedicationFormChips(selected = form, onSelect = { form = it })
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("USUAL DOSE", color = palette.fgMuted, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    DoseFields(amount, unit, { amount = it }, { unit = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, form, amount, unit) },
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
