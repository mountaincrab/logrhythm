package com.mountaincrab.logrhythm.ui.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.ui.components.FieldLabel
import com.mountaincrab.logrhythm.ui.components.SaveBar
import com.mountaincrab.logrhythm.ui.components.SheetHeader
import com.mountaincrab.logrhythm.ui.components.WhenPicker
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddFoodScreen(
    editId: String?,
    onDismiss: () -> Unit,
    viewModel: AddFoodViewModel = koinViewModel(parameters = { parametersOf(editId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SheetHeader(title = if (editId == null) "Log food" else "Edit food", onClose = onDismiss)

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
                FieldLabel("What you ate")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceRaised)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .defaultMinSize(minHeight = 120.dp),
                ) {
                    if (state.items.isEmpty()) {
                        Text(
                            text = "Free text — be specific where it matters (e.g. 'spicy chicken', 'whole milk').",
                            color = palette.fgFaint, fontSize = 14.sp,
                        )
                    }
                    BasicTextField(
                        value = state.items,
                        onValueChange = viewModel::onItemsChange,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.recent.isNotEmpty()) {
                Column(modifier = Modifier.padding(bottom = 18.dp)) {
                    FieldLabel("Recent", hint = "tap to add")
                    FlowRow(state.recent) { chip ->
                        RecentChip(chip) { viewModel.appendChip(chip) }
                    }
                }
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("Tag", hint = "optional")
                FlowRow(MealTag.entries.toList()) { tag ->
                    MealTagChip(
                        tag = tag,
                        selected = state.mealTag == tag,
                        onClick = { viewModel.onMealTagToggle(tag) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        SaveBar(
            onCancel = onDismiss,
            onSave = viewModel::save,
            saveLabel = "Save food",
            saveEnabled = !state.saving && state.items.isNotBlank(),
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowRow(items: List<T>, item: @Composable (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item(it) }
    }
}

@Composable
private fun RecentChip(text: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, color = palette.fgMuted, fontSize = 12.sp)
    }
}

@Composable
private fun MealTagChip(tag: MealTag, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val icon: ImageVector = when (tag) {
        MealTag.BREAKFAST -> Icons.Outlined.WbSunny
        MealTag.LUNCH -> Icons.Outlined.LocalDining
        MealTag.DINNER -> Icons.Outlined.Nightlight
        MealTag.SNACK -> Icons.Outlined.Cookie
        MealTag.DRINK -> Icons.Outlined.LocalDrink
    }
    val tintFg = if (selected) palette.accentText else palette.fgMuted
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.accentSoft else palette.surfaceRaised)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else palette.border,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tintFg, modifier = Modifier.size(14.dp))
        Text(tag.label, color = tintFg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
