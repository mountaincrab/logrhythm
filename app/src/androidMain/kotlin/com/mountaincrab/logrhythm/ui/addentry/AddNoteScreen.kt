package com.mountaincrab.logrhythm.ui.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.ExtrasTagEntity
import com.mountaincrab.logrhythm.ui.components.FieldLabel
import com.mountaincrab.logrhythm.ui.components.SaveBar
import com.mountaincrab.logrhythm.ui.components.SheetHeader
import com.mountaincrab.logrhythm.ui.components.WhenPicker
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddNoteScreen(
    editId: String?,
    onDismiss: () -> Unit,
    viewModel: AddNoteViewModel = koinViewModel(parameters = { parametersOf(editId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allExtrasTags by viewModel.allExtrasTags.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SheetHeader(title = if (editId == null) "Log note" else "Edit note", onClose = onDismiss)

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
                FieldLabel("Note", hint = "optional")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceRaised)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .defaultMinSize(minHeight = 120.dp),
                ) {
                    if (state.content.isEmpty()) {
                        Text(
                            text = "Symptoms, mood, context, anything worth recording…",
                            color = palette.fgFaint, fontSize = 14.sp,
                        )
                    }
                    BasicTextField(
                        value = state.content,
                        onValueChange = viewModel::onContentChange,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("Tags")
                ExtrasTagsRow(
                    tags = allExtrasTags,
                    selectedIds = state.selectedExtrasTagIds,
                    onToggle = viewModel::onExtrasTagToggle,
                    onAddNew = viewModel::createExtrasTagAndSelect,
                )
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("Lifestyle")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LifestyleCard(
                        title = "Caffeine",
                        subtitle = if (state.caffeine) "Yes today" else "Not today",
                        on = state.caffeine,
                        onClick = viewModel::onCaffeineToggle,
                        modifier = Modifier.weight(1f),
                    )
                    LifestyleCard(
                        title = "Alcohol",
                        subtitle = if (state.alcohol) "Yes today" else "Not today",
                        on = state.alcohol,
                        onClick = viewModel::onAlcoholToggle,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SaveBar(
            onCancel = onDismiss,
            onSave = viewModel::save,
            saveLabel = "Save",
            saveEnabled = !state.saving && (state.content.isNotBlank() || state.caffeine || state.alcohol || state.selectedExtrasTagIds.isNotEmpty()),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtrasTagsRow(
    tags: List<ExtrasTagEntity>,
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
                    placeholder = { Text("e.g. No suppository") },
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surfaceRaised)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                .clickable { showDialog = true }
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = "+ New tag",
                color = palette.fgMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LifestyleCard(
    title: String,
    subtitle: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) palette.accentSoft else palette.surfaceRaised)
            .border(
                1.dp,
                if (on) MaterialTheme.colorScheme.primary else palette.border,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle,
                color = if (on) palette.accentText else palette.fgMuted,
                fontSize = 11.sp)
        }
    }
}
