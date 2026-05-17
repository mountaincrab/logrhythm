package com.mountaincrab.logrhythm.ui.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.StoolTagEntity
import com.mountaincrab.logrhythm.data.model.BRISTOL_TYPES
import com.mountaincrab.logrhythm.ui.components.FieldLabel
import com.mountaincrab.logrhythm.ui.components.SaveBar
import com.mountaincrab.logrhythm.ui.components.SheetHeader
import com.mountaincrab.logrhythm.ui.components.WhenPicker
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.theme.RatingColors
import org.koin.core.parameter.parametersOf
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddPoopScreen(
    editId: String?,
    onDismiss: () -> Unit,
    viewModel: AddPoopViewModel = koinViewModel(parameters = { parametersOf(editId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SheetHeader(title = if (editId == null) "Log a poop" else "Edit poop", onClose = onDismiss)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            // When
            FieldGroup {
                FieldLabel("When", hint = "Now")
                WhenPicker(occurredAt = state.occurredAt, onChange = viewModel::onOccurredAtChange)
            }

            // Type
            FieldGroup {
                FieldLabel("Type")
                BristolGrid(
                    selected = state.bristolTypes,
                    onToggle = viewModel::onBristolToggle,
                )
                Spacer(modifier = Modifier.height(12.dp))
                PersonalTagsRow(
                    tags = allTags,
                    selectedIds = state.selectedTagIds,
                    onToggle = viewModel::onTagToggle,
                    onAddNew = viewModel::createTagAndSelect,
                )
            }

            // Rating
            FieldGroup {
                FieldLabel("Blood")
                RatingPills(selected = state.blood, onSelect = viewModel::onBloodChange)
                val rc = RatingColors[state.blood]
                if (rc != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = rc.bg,
                            modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(rc.label, color = rc.bg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Notes
            FieldGroup {
                FieldLabel("Notes", hint = "optional")
                NotesField(value = state.notes, onChange = viewModel::onNotesChange,
                    placeholder = "Urgency, pain, time of day, anything that felt different…")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SaveBar(
            onCancel = onDismiss,
            onSave = viewModel::save,
            saveLabel = "Save poop",
            saveEnabled = !state.saving,
        )
    }
}

@Composable
private fun FieldGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) { content() }
}

@Composable
private fun BristolGrid(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val palette = LocalAppPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        BRISTOL_TYPES.forEach { type ->
            val isOn = type.n in selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOn) palette.accentSoft else palette.surfaceRaised)
                    .border(
                        1.dp,
                        if (isOn) MaterialTheme.colorScheme.primary else palette.border,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onToggle(type.n) }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${type.n}",
                    color = if (isOn) palette.accentText else MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = type.plain.split(' ').first(),
                    color = palette.fgMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RatingPills(selected: Int, onSelect: (Int) -> Unit) {
    val palette = LocalAppPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        (1..5).forEach { n ->
            val c = RatingColors.getValue(n)
            val isOn = n == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isOn) c.bg else palette.surfaceRaised)
                    .border(
                        1.5.dp,
                        if (isOn) c.bg else palette.border,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(n) },
            ) {
                if (!isOn) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(c.bg)
                            .align(Alignment.TopEnd),
                    )
                }
                Text(
                    text = "$n",
                    modifier = Modifier.align(Alignment.Center),
                    color = if (isOn) c.fg else MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun NotesField(value: String, onChange: (String) -> Unit, placeholder: String) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .defaultMinSize(minHeight = 80.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = palette.fgFaint, fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PersonalTagsRow(
    tags: List<StoolTagEntity>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onAddNew: (String) -> Unit,
) {
    val palette = LocalAppPalette.current
    var showDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; newTagName = "" },
            title = { Text("New tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("e.g. Tiny bits") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) onAddNew(newTagName)
                        showDialog = false
                        newTagName = ""
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; newTagName = "" }) { Text("Cancel") }
            },
        )
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag ->
            val on = tag.id in selectedIds
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) palette.accentSoft else palette.surfaceRaised)
                    .border(
                        1.dp,
                        if (on) MaterialTheme.colorScheme.primary else palette.border,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onToggle(tag.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = tag.name,
                    color = if (on) palette.accentText else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
        // "+ New tag" chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surfaceRaised)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                .clickable { showDialog = true }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = palette.fgMuted,
                    modifier = Modifier.size(13.dp),
                )
                Text("New tag", color = palette.fgMuted, fontSize = 13.sp)
            }
        }
    }
}
