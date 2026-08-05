package com.mountaincrab.logrhythm.ui.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.ui.components.FieldLabel
import com.mountaincrab.logrhythm.ui.components.SaveBar
import com.mountaincrab.logrhythm.ui.components.SheetHeader
import com.mountaincrab.logrhythm.ui.components.WhenPicker
import com.mountaincrab.logrhythm.ui.meds.MedicationEditorDialog
import com.mountaincrab.logrhythm.ui.meds.MedicationPicker
import com.mountaincrab.logrhythm.ui.meds.PlainInput
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddMedicineScreen(
    editId: String?,
    onDismiss: () -> Unit,
    viewModel: AddMedicineViewModel = koinViewModel(parameters = { parametersOf(editId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current
    var showNewMedication by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }

    if (showNewMedication) {
        MedicationEditorDialog(
            initial = null,
            onConfirm = { name, form, dose ->
                viewModel.createMedicationAndSelect(name, form, dose)
                showNewMedication = false
            },
            onDismiss = { showNewMedication = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SheetHeader(title = if (editId == null) "Log medicine" else "Edit dose", onClose = onDismiss)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("When", hint = "Now")
                WhenPicker(occurredAt = state.occurredAt, onChange = viewModel::onOccurredAtChange)
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("Medication")
                if (medications.isEmpty()) {
                    Text(
                        text = "No medications yet — add one to log a dose.",
                        color = palette.fgMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                MedicationPicker(
                    medications = medications,
                    selectedId = state.medicationId,
                    onSelect = viewModel::selectMedication,
                    onCreateNew = { showNewMedication = true },
                )
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                // How many units — the strength itself is part of the medication's definition.
                val strength = medications.firstOrNull { it.id == state.medicationId }
                    ?.dose?.takeIf { it.isNotBlank() }
                FieldLabel("Quantity", hint = strength?.let { "× $it" })
                PlainInput(
                    value = state.quantity,
                    onValueChange = viewModel::onQuantityChange,
                    placeholder = "e.g. 2",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("Note", hint = "optional")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceRaised)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .defaultMinSize(minHeight = 80.dp),
                ) {
                    if (state.notes.isEmpty()) {
                        Text(text = "Why you took it, how it went…", color = palette.fgFaint, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SaveBar(
            onCancel = onDismiss,
            onSave = viewModel::save,
            saveLabel = "Save",
            saveEnabled = !state.saving && state.medicationId != null,
        )
    }
}
