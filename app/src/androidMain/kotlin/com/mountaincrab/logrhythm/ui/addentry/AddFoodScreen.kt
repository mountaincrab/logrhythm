@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mountaincrab.logrhythm.ui.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.FoodItemWithComponents
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.data.local.entity.formatFoodNumber
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
    onOpenFoodLibrary: () -> Unit,
    viewModel: AddFoodViewModel = koinViewModel(parameters = { parametersOf(editId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items by viewModel.foodItems.collectAsStateWithLifecycle()
    val lookupItems by viewModel.foodItemsForLookup.collectAsStateWithLifecycle()
    val components by viewModel.components.collectAsStateWithLifecycle()
    val lookupComponents by viewModel.componentsForLookup.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var displayLines by remember { mutableStateOf(state.lines) }
    var draggingLineId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val lineHeights = remember { mutableStateMapOf<String, Float>() }
    val density = LocalDensity.current
    val lineSpacingPx = with(density) { 8.dp.toPx() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }
    LaunchedEffect(state.lines) {
        if (draggingLineId == null) displayLines = state.lines
    }

    if (showPicker) {
        FoodItemPickerDialog(
            items = items,
            components = components,
            onSavedItem = { viewModel.addSavedItem(it); showPicker = false },
            onCustomItem = { text, amounts -> viewModel.addCustomItem(text, amounts); showPicker = false },
            onOpenFoodLibrary = { showPicker = false; onOpenFoodLibrary() },
            onDismiss = { showPicker = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SheetHeader(title = if (editId == null) "Log food" else "Edit food", onClose = onDismiss)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("When", hint = "Now")
                WhenPicker(occurredAt = state.occurredAt, onChange = viewModel::onOccurredAtChange)
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("What you had", hint = "${state.lines.size} item${if (state.lines.size == 1) "" else "s"}")
                val itemMap = lookupItems.associateBy { it.item.id }
                val componentMap = lookupComponents.associateBy { it.id }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayLines.forEach { line ->
                        key(line.id) {
                            val isDragging = draggingLineId == line.id
                            FoodLineCard(
                                draft = line,
                                item = line.foodItemId?.let(itemMap::get),
                                components = componentMap,
                                onQuantity = { viewModel.updateQuantity(line.id, it) },
                                onDecrement = { viewModel.adjustQuantity(line.id, -1) },
                                onIncrement = { viewModel.adjustQuantity(line.id, 1) },
                                onRemove = { viewModel.removeLine(line.id) },
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                        alpha = if (isDragging) 0.85f else 1f
                                    }
                                    .onSizeChanged { size ->
                                        if (size.height > 0) lineHeights[line.id] = size.height.toFloat()
                                    }
                                    .pointerInput(line.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                draggingLineId = line.id
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val fallbackSlotHeight =
                                                    (lineHeights[line.id] ?: with(density) { 80.dp.toPx() }) + lineSpacingPx
                                                var currentIndex = displayLines.indexOfFirst { it.id == line.id }
                                                if (currentIndex < 0) return@detectDragGesturesAfterLongPress

                                                while (currentIndex < displayLines.lastIndex) {
                                                    val neighbour = displayLines[currentIndex + 1]
                                                    val neighbourSlotHeight =
                                                        (lineHeights[neighbour.id] ?: fallbackSlotHeight - lineSpacingPx) + lineSpacingPx
                                                    if (dragOffsetY <= neighbourSlotHeight / 2f) break
                                                    displayLines = displayLines.toMutableList().apply {
                                                        add(currentIndex + 1, removeAt(currentIndex))
                                                    }
                                                    dragOffsetY -= neighbourSlotHeight
                                                    currentIndex++
                                                }
                                                while (currentIndex > 0) {
                                                    val neighbour = displayLines[currentIndex - 1]
                                                    val neighbourSlotHeight =
                                                        (lineHeights[neighbour.id] ?: fallbackSlotHeight - lineSpacingPx) + lineSpacingPx
                                                    if (dragOffsetY >= -neighbourSlotHeight / 2f) break
                                                    displayLines = displayLines.toMutableList().apply {
                                                        add(currentIndex - 1, removeAt(currentIndex))
                                                    }
                                                    dragOffsetY += neighbourSlotHeight
                                                    currentIndex--
                                                }
                                            },
                                            onDragEnd = {
                                                val id = draggingLineId
                                                val targetIndex = id?.let { draggedId ->
                                                    displayLines.indexOfFirst { it.id == draggedId }
                                                } ?: -1
                                                draggingLineId = null
                                                dragOffsetY = 0f
                                                if (id != null && targetIndex >= 0) {
                                                    viewModel.reorderLine(id, targetIndex)
                                                }
                                            },
                                            onDragCancel = {
                                                draggingLineId = null
                                                dragOffsetY = 0f
                                                displayLines = state.lines
                                            },
                                        )
                                    },
                            )
                        }
                    }
                    AddOutlineButton("Add item") { showPicker = true }
                }
            }

            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                FieldLabel("Tag", hint = "optional")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealTag.entries.forEach { tag ->
                        val selected = state.mealTag == tag
                        val palette = LocalAppPalette.current
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                .background(if (selected) palette.accentSoft else palette.surfaceRaised)
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else palette.border, RoundedCornerShape(12.dp))
                                .clickable { viewModel.onMealTagToggle(tag) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(tag.label, color = if (selected) palette.accentText else palette.fgMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        SaveBar(
            onCancel = onDismiss,
            onSave = viewModel::save,
            saveLabel = "Save food",
            saveEnabled = !state.saving && state.lines.isNotEmpty() && state.lines.all {
                if (it.foodItemId != null) it.quantity.toDoubleOrNull()?.let { n -> n.isFinite() && n >= 1.0 } == true
                else !it.customText.isNullOrBlank()
            },
        )
    }
}

@Composable
private fun FoodLineCard(
    draft: FoodLineDraft,
    item: FoodItemWithComponents?,
    components: Map<String, TrackedComponentEntity>,
    onQuantity: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised).border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item?.item?.icon ?: "🍴", fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item?.item?.name ?: draft.customText ?: "Unavailable food item",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item?.item?.let {
                    Text(
                        "${it.amount} ${it.unit} each",
                        color = palette.fgMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item == null && draft.foodItemId == null) Text("Custom item", color = palette.fgMuted, fontSize = 12.sp)
            }
            if (draft.foodItemId != null) {
                QuantityStepper(
                    value = draft.quantity,
                    onValueChange = onQuantity,
                    onDecrement = onDecrement,
                    onIncrement = onIncrement,
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove item", tint = palette.dangerText)
            }
        }
        val quantity = draft.quantity.toDoubleOrNull() ?: 1.0
        val amounts = item?.components?.associate { it.componentId to it.amount * quantity }
            ?: draft.componentAmounts.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
        if (amounts.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                amounts.filterValues { it > 0.0 }.forEach { (componentId, amount) ->
                    components[componentId]?.let { component ->
                        Box(Modifier.clip(RoundedCornerShape(999.dp)).background(palette.accentSoft).padding(horizontal = 9.dp, vertical = 4.dp)) {
                            Text("${component.name} ${formatFoodNumber(amount)} ${component.unit}", color = palette.accentText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    value: String,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val quantity = value.toDoubleOrNull()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Qty", color = palette.fgMuted, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.height(36.dp).clip(RoundedCornerShape(9.dp))
                .border(1.dp, palette.border, RoundedCornerShape(9.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = quantity?.let { it.isFinite() && it > 1.0 } == true,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "Decrease quantity",
                    tint = if (quantity?.let { it.isFinite() && it > 1.0 } == true) palette.fgMuted else palette.fgDisabled,
                    modifier = Modifier.size(16.dp),
                )
            }
            VerticalDivider(modifier = Modifier.height(20.dp), color = palette.borderSubtle)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.width(38.dp).fillMaxHeight()
                    .semantics { contentDescription = "Quantity" }
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && value.toDoubleOrNull()?.let { it.isFinite() && it >= 1.0 } != true) {
                            onValueChange("1")
                        }
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerField ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { innerField() }
                },
            )
            VerticalDivider(modifier = Modifier.height(20.dp), color = palette.borderSubtle)
            IconButton(onClick = onIncrement, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Increase quantity",
                    tint = palette.fgMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AddOutlineButton(label: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = palette.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FoodItemPickerDialog(
    items: List<FoodItemWithComponents>,
    components: List<TrackedComponentEntity>,
    onSavedItem: (String) -> Unit,
    onCustomItem: (String, Map<String, String>) -> Unit,
    onOpenFoodLibrary: () -> Unit,
    onDismiss: () -> Unit,
) {
    var custom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var amounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var query by remember { mutableStateOf("") }
    val palette = LocalAppPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(3.dp)) {
                    listOf(false to "Saved", true to "Custom").forEach { (value, label) ->
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                .background(if (custom == value) palette.surfaceHigh else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { custom = value }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, color = if (custom == value) MaterialTheme.colorScheme.onSurface else palette.fgMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    }
                }
                if (!custom) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search saved items") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (items.isEmpty()) Text("No saved items yet. Add one in Settings → Food library.", color = palette.fgMuted, fontSize = 13.sp)
                        items.filter { query.isBlank() || it.item.name.contains(query.trim(), ignoreCase = true) }.forEach { food ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(palette.surfaceRaised)
                                    .border(1.dp, palette.border, RoundedCornerShape(12.dp)).clickable { onSavedItem(food.item.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(food.item.icon, fontSize = 22.sp)
                                Column {
                                    Text(food.item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${food.item.amount} ${food.item.unit}", color = palette.fgMuted, fontSize = 12.sp)
                                }
                            }
                        }
                        TextButton(onClick = onOpenFoodLibrary, modifier = Modifier.fillMaxWidth()) {
                            Text(if (items.isEmpty()) "Create a food item" else "Manage food library")
                        }
                    }
                } else {
                    OutlinedTextField(customText, { customText = it }, label = { Text("What you had") }, modifier = Modifier.fillMaxWidth())
                    components.forEach { component ->
                        OutlinedTextField(
                            value = amounts[component.id].orEmpty(),
                            onValueChange = { value -> amounts = amounts + (component.id to value) },
                            label = { Text("${component.name} (${component.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (custom) TextButton(onClick = { onCustomItem(customText, amounts) }, enabled = customText.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
