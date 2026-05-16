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
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.model.BRISTOL_TYPES
import com.mountaincrab.logrhythm.data.model.StoolSystem
import com.mountaincrab.logrhythm.data.model.bristol
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
    val stoolSystem by viewModel.stoolSystem.collectAsStateWithLifecycle()
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
                StoolSystemToggle(
                    current = stoolSystem,
                    onSelect = viewModel::onStoolSystemChange,
                )
                Spacer(modifier = Modifier.height(10.dp))
                BristolGrid(
                    selected = state.bristolTypes,
                    stoolSystem = stoolSystem,
                    onToggle = viewModel::onBristolToggle,
                )
                val selectedTypes = state.bristolTypes.sorted()
                    .mapNotNull { runCatching { bristol(it) }.getOrNull() }
                if (selectedTypes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildAnnotatedTypeDescription(selectedTypes.map { it.n to it.plain }, stoolSystem),
                        color = palette.fgMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }

            // Rating
            FieldGroup {
                FieldLabel("Blood rating", hint = "1–5")
                RatingPills(selected = state.rating, onSelect = viewModel::onRatingChange)
                val rc = RatingColors[state.rating]
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
private fun StoolSystemToggle(current: StoolSystem, onSelect: (StoolSystem) -> Unit) {
    val palette = LocalAppPalette.current
    val options = listOf("Bristol scale" to StoolSystem.BRISTOL, "My types" to StoolSystem.PLAIN)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (label, sys) ->
            val on = sys == current || (current == StoolSystem.BOTH && sys == StoolSystem.BRISTOL)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) palette.surfaceHigh else Color.Transparent)
                    .clickable { onSelect(sys) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (on) MaterialTheme.colorScheme.onSurface else palette.fgMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun BristolGrid(selected: Set<Int>, stoolSystem: StoolSystem, onToggle: (Int) -> Unit) {
    val palette = LocalAppPalette.current
    val usePlain = stoolSystem == StoolSystem.PLAIN
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
                if (usePlain) {
                    Text(
                        text = type.plain.split(' ').first(),
                        color = if (isOn) palette.accentText else MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "${type.n}",
                        color = palette.fgMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
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

private fun buildAnnotatedTypeDescription(
    types: List<Pair<Int, String>>,
    stoolSystem: StoolSystem,
): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        types.forEachIndexed { index, (n, plain) ->
            if (index > 0) append("  ·  ")
            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold))
            if (stoolSystem != StoolSystem.PLAIN) append("Type $n · ")
            append(plain)
            pop()
        }
    }
