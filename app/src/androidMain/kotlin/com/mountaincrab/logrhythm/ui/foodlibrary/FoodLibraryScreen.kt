@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mountaincrab.logrhythm.ui.foodlibrary

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.DEFAULT_FOOD_ITEM_ICON
import com.mountaincrab.logrhythm.data.local.entity.FoodItemWithComponents
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import org.koin.compose.viewmodel.koinViewModel

private enum class LibraryTab(val label: String) { ITEMS("Items"), COMPONENTS("Components") }

@Composable
fun FoodLibraryScreen(onBack: () -> Unit, viewModel: FoodLibraryViewModel = koinViewModel()) {
    var tab by remember { mutableStateOf(LibraryTab.ITEMS) }
    var editingItem by remember { mutableStateOf<FoodItemWithComponents?>(null) }
    var editingComponent by remember { mutableStateOf<TrackedComponentEntity?>(null) }
    var showItemEditor by remember { mutableStateOf(false) }
    var showComponentEditor by remember { mutableStateOf(false) }
    val items by viewModel.items.collectAsStateWithLifecycle()
    val archivedItems by viewModel.archivedItems.collectAsStateWithLifecycle()
    val components by viewModel.components.collectAsStateWithLifecycle()
    val archivedComponents by viewModel.archivedComponents.collectAsStateWithLifecycle()
    val lockedComponentIds by viewModel.lockedComponentIds.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    if (showComponentEditor) ComponentEditorDialog(
        initial = editingComponent,
        unitLocked = editingComponent?.id in lockedComponentIds,
        existingComponents = components,
        onDismiss = { showComponentEditor = false; editingComponent = null },
        onSave = { name, unit -> viewModel.saveComponent(editingComponent?.id, name, unit); showComponentEditor = false; editingComponent = null },
    )
    if (showItemEditor) FoodItemEditorDialog(
        initial = editingItem,
        components = components,
        onDismiss = { showItemEditor = false; editingItem = null },
        onSave = { name, icon, amount, unit, values -> viewModel.saveItem(editingItem?.item?.id, name, icon, amount, unit, values); showItemEditor = false; editingItem = null },
    )

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = palette.fgMuted) }
            Column {
                Text("Food library", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
                Text("Saved items and what they contain", color = palette.fgMuted, fontSize = 13.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, palette.border, RoundedCornerShape(12.dp)).padding(3.dp)) {
            LibraryTab.entries.forEach { value ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (tab == value) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent).clickable { tab = value }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(value.label, color = if (tab == value) MaterialTheme.colorScheme.onPrimary else palette.fgMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (tab) {
                LibraryTab.ITEMS -> {
                    items(items, key = { it.item.id }) { food ->
                        LibraryRow(
                            title = food.item.name,
                            leadingIcon = food.item.icon,
                            subtitle = buildString {
                                append("${food.item.amount} ${food.item.unit}")
                                if (food.components.isNotEmpty()) append(" · ${food.components.size} component${if (food.components.size == 1) "" else "s"}")
                            },
                            onClick = { editingItem = food; showItemEditor = true },
                            onArchive = { viewModel.archiveItem(food.item.id) },
                        )
                    }
                    item { AddRow("+ Add food item") { editingItem = null; showItemEditor = true } }
                    if (archivedItems.isNotEmpty()) {
                        item { SectionLabel("Archived") }
                        items(archivedItems, key = { "archived-${it.item.id}" }) { food ->
                            ArchivedRow(food.item.name, "${food.item.amount} ${food.item.unit}") { viewModel.restoreItem(food.item.id) }
                        }
                    }
                }
                LibraryTab.COMPONENTS -> {
                    items(components, key = { it.id }) { component ->
                        LibraryRow(
                            title = component.name,
                            subtitle = component.unit,
                            onClick = { editingComponent = component; showComponentEditor = true },
                            onArchive = { viewModel.archiveComponent(component.id) },
                        )
                    }
                    item { AddRow("+ Add component") { editingComponent = null; showComponentEditor = true } }
                    if (archivedComponents.isNotEmpty()) {
                        item { SectionLabel("Archived") }
                        items(archivedComponents, key = { "archived-${it.id}" }) { component ->
                            ArchivedRow(component.name, component.unit) { viewModel.restoreComponent(component.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(title: String, subtitle: String, onClick: () -> Unit, onArchive: () -> Unit, leadingIcon: String? = null) {
    val palette = LocalAppPalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.surfaceRaised).border(1.dp, palette.border, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        leadingIcon?.let {
            Text(it, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
        }
        Column(Modifier.weight(1f)) { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = palette.fgMuted, fontSize = 12.sp) }
        IconButton(onClick = onArchive) { Icon(Icons.Outlined.Archive, "Archive", tint = palette.fgMuted) }
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(13.dp), contentAlignment = Alignment.Center) {
        Text(label, color = palette.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun SectionLabel(text: String) = Text(text.uppercase(), color = LocalAppPalette.current.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 12.dp, start = 4.dp))

@Composable
private fun ArchivedRow(title: String, subtitle: String, onRestore: () -> Unit) {
    val palette = LocalAppPalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(palette.surfaceRaised).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = palette.fgMuted, fontWeight = FontWeight.SemiBold); Text(subtitle, color = palette.fgFaint, fontSize = 12.sp) }
        IconButton(onClick = onRestore) { Icon(Icons.Outlined.Restore, "Restore", tint = palette.accentText) }
    }
}

@Composable
private fun ComponentEditorDialog(
    initial: TrackedComponentEntity?,
    unitLocked: Boolean,
    existingComponents: List<TrackedComponentEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var unit by remember(initial?.id) { mutableStateOf(initial?.unit.orEmpty()) }
    val duplicateName = existingComponents.any { it.id != initial?.id && it.name.equals(name.trim(), ignoreCase = true) }
    val fieldColors = foodDialogFieldColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add component" else "Edit component") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors, shape = RoundedCornerShape(12.dp), singleLine = true)
            if (duplicateName) Text("A component with this name already exists", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            OutlinedTextField(
                unit,
                { unit = it },
                label = { Text("Unit") },
                enabled = !unitLocked,
                supportingText = { Text(if (unitLocked) "Locked because this component is already used" else "Used consistently in food items and trends") },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
        } },
        confirmButton = { TextButton(onClick = { onSave(name, unit) }, enabled = name.isNotBlank() && unit.isNotBlank() && !duplicateName) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun FoodItemEditorDialog(initial: FoodItemWithComponents?, components: List<TrackedComponentEntity>, onDismiss: () -> Unit, onSave: (String, String, String, String, Map<String, Double>) -> Unit) {
    var name by remember(initial?.item?.id) { mutableStateOf(initial?.item?.name.orEmpty()) }
    var icon by remember(initial?.item?.id) { mutableStateOf(initial?.item?.icon ?: DEFAULT_FOOD_ITEM_ICON) }
    var amount by remember(initial?.item?.id) { mutableStateOf(initial?.item?.amount.orEmpty()) }
    var unit by remember(initial?.item?.id) { mutableStateOf(initial?.item?.unit.orEmpty()) }
    var values by remember(initial?.item?.id) { mutableStateOf(initial?.components?.associate { it.componentId to it.amount.toString().removeSuffix(".0") }.orEmpty()) }
    val fieldColors = foodDialogFieldColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add food item" else "Edit food item") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it.firstUnicodeCharacter() },
                    label = { Text("Icon") },
                    supportingText = { Text("One character") },
                    modifier = Modifier.width(104.dp),
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f), colors = fieldColors, shape = RoundedCornerShape(12.dp), singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), colors = fieldColors, shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), colors = fieldColors, shape = RoundedCornerShape(12.dp), singleLine = true)
            }
            Text("TRACKED COMPONENTS · PER ITEM", color = LocalAppPalette.current.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            components.forEach { component ->
                OutlinedTextField(
                    value = values[component.id].orEmpty(),
                    onValueChange = { values = values + (component.id to it) },
                    label = { Text("${component.name} (${component.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors, shape = RoundedCornerShape(12.dp), singleLine = true,
                )
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(name, icon, amount, unit, values.mapNotNull { (id, raw) -> raw.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { id to it } }.toMap()) }, enabled = icon.isNotBlank() && name.isNotBlank() && amount.isNotBlank() && unit.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun foodDialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = LocalAppPalette.current.borderStrong,
    disabledBorderColor = LocalAppPalette.current.border,
    focusedContainerColor = LocalAppPalette.current.surfaceRaised,
    unfocusedContainerColor = LocalAppPalette.current.surfaceRaised,
    disabledContainerColor = LocalAppPalette.current.surfaceRaised,
)

private fun String.firstUnicodeCharacter(): String {
    val value = trimStart()
    if (value.isEmpty()) return ""
    return value.substring(0, value.offsetByCodePoints(0, 1))
}
