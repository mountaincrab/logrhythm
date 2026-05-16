package com.mountaincrab.logrhythm.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.model.bristol
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.theme.RatingColors
import com.mountaincrab.logrhythm.ui.util.formatFullDay
import com.mountaincrab.logrhythm.ui.util.formatFullDayWithTime
import com.mountaincrab.logrhythm.ui.util.formatTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EntryDetailScreen(
    kind: String,
    id: String,
    onBack: () -> Unit,
    onEdit: (kind: String, id: String) -> Unit,
    viewModel: EntryDetailViewModel = koinViewModel(parameters = { parametersOf(kind, id) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Header(
            title = when (kind) {
                "poop" -> state.poop?.occurredAt?.formatFullDayWithTime()
                "food" -> state.food?.occurredAt?.formatFullDayWithTime()
                else -> state.note?.occurredAt?.formatFullDayWithTime()
            } ?: "Entry",
            onBack = onBack,
            onEdit = { onEdit(kind, id) },
            onDelete = viewModel::delete,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 20.dp),
        ) {
            when (kind) {
                "poop" -> state.poop?.let { PoopDetail(it, state.foodWindow.map { f ->
                    FoodRow(time = f.occurredAt.formatTime(), items = f.items)
                }) }
                "food" -> state.food?.let { f ->
                    DetailNotesCard("Time", f.occurredAt.formatTime())
                    DetailNotesCard("Date", f.occurredAt.formatFullDay())
                    DetailNotesCard("What you ate", f.items)
                    f.mealTag?.let { DetailNotesCard("Tag", it.label) }
                }
                "note" -> state.note?.let { n ->
                    DetailNotesCard("Time", n.occurredAt.formatTime())
                    DetailNotesCard("Date", n.occurredAt.formatFullDay())
                    DetailNotesCard("Note", n.content)
                }
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(Icons.Outlined.ArrowBack, onBack)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ENTRY", color = palette.fgMuted, fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
            Text(title, color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconBtn(Icons.Outlined.Edit, onEdit)
            IconBtn(Icons.Outlined.DeleteOutline, onDelete)
        }
    }
}

@Composable
private fun IconBtn(icon: ImageVector, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = palette.fgMuted, modifier = Modifier.size(20.dp))
    }
}

data class FoodRow(val time: String, val items: String)

@Composable
private fun PoopDetail(p: PoopEntryEntity, foods: List<FoodRow>) {
    val palette = LocalAppPalette.current
    val rc = RatingColors[p.rating] ?: RatingColors.getValue(1)

    // Hero
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(rc.bg),
            contentAlignment = Alignment.Center,
        ) {
            Text("${p.rating}", color = rc.fg,
                fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("BLOOD RATING", color = palette.fgMuted,
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
            Text(rc.label, color = rc.bg,
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    // Two-column time + bristol
    val br = runCatching { bristol(p.bristol) }.getOrNull()
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TwoColCard("TIME", p.occurredAt.formatTime(), p.occurredAt.formatFullDay(), Modifier.weight(1f))
        TwoColCard("STOOL", "Bristol ${p.bristol}", br?.let { "${it.plain} · ${it.description}" }, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))

    if (!p.notes.isNullOrBlank()) {
        DetailNotesCard("Notes", p.notes)
    }

    if (foods.isNotEmpty()) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceRaised)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text("FOOD IN THE 24H BEFORE", color = palette.fgMuted,
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            foods.forEachIndexed { i, f ->
                if (i > 0) Box(modifier = Modifier
                    .fillMaxWidth().height(1.dp).background(palette.borderSubtle))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(f.time, modifier = Modifier.width(52.dp),
                        color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(f.items, modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Other context chips
    val flags = listOfNotNull(
        if (!p.medsMissed) "No meds missed" else "Meds missed",
        if (!p.caffeine) "No caffeine" else "Caffeine",
        if (!p.alcohol) "No alcohol" else "Alcohol",
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text("OTHER", color = palette.fgMuted,
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            flags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.surfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(tag, color = palette.fgMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TwoColCard(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, color = palette.fgMuted, fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 4.dp))
        if (sub != null) {
            Text(sub, color = palette.fgMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DetailNotesCard(label: String, value: String) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label.uppercase(), color = palette.fgMuted,
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp, lineHeight = 21.sp,
            modifier = Modifier.padding(top = 4.dp))
    }
    Spacer(modifier = Modifier.height(12.dp))
}
