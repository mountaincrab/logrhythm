package com.mountaincrab.logrhythm.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.ui.components.BottomTabBar
import com.mountaincrab.logrhythm.ui.navigation.Screen
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.theme.RatingColors
import org.koin.compose.viewmodel.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
private val axisDateFmt = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun HistoryScreen(
    onTabSelect: (route: String) -> Unit,
    onOpenEntry: (kind: String, id: String) -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        ) {
            Text(text = "History", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp, color = MaterialTheme.colorScheme.onBackground)
            val sub = when (state.tab) {
                HistoryTab.CALENDAR -> "${state.daysLoggedThisMonth} days logged this month"
                HistoryTab.TRENDS -> {
                    val avg = state.ratingAvg
                    val rangeLabel = state.range.label
                    if (avg != null) "Last $rangeLabel · avg rating ${"%.1f".format(avg)}"
                    else "Last $rangeLabel · no poops logged"
                }
            }
            Text(sub, fontSize = 13.sp, color = palette.fgMuted)
        }

        // Sub-tabs
        SubTabs(tab = state.tab, onSelect = viewModel::selectTab)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            when (state.tab) {
                HistoryTab.CALENDAR -> CalendarView(state, viewModel)
                HistoryTab.TRENDS -> TrendsView(state, viewModel)
            }
        }

        BottomTabBar(active = Screen.History.route, onSelect = onTabSelect)
    }
}

@Composable
private fun SubTabs(tab: HistoryTab, onSelect: (HistoryTab) -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SubTab(active = tab == HistoryTab.CALENDAR, icon = Icons.Outlined.CalendarMonth,
            label = "Calendar", modifier = Modifier.weight(1f)) { onSelect(HistoryTab.CALENDAR) }
        SubTab(active = tab == HistoryTab.TRENDS, icon = Icons.Outlined.TrendingUp,
            label = "Trends", modifier = Modifier.weight(1f)) { onSelect(HistoryTab.TRENDS) }
    }
}

@Composable
private fun SubTab(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.onPrimary else palette.fgMuted,
            modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label,
            color = if (active) MaterialTheme.colorScheme.onPrimary else palette.fgMuted,
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarView(state: HistoryUiState, viewModel: HistoryViewModel) {
    val palette = LocalAppPalette.current

    // Month head
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(state.month.format(monthFmt),
            modifier = Modifier.weight(1f),
            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconBtn(Icons.Outlined.ChevronLeft, viewModel::previousMonth)
            IconBtn(Icons.Outlined.ChevronRight, viewModel::nextMonth)
        }
    }

    // Calendar card
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        // weekday header
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { wd ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(wd, color = palette.fgFaint, fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        val firstDow = state.month.atDay(1).dayOfWeek
        // 0 = Mon, 6 = Sun
        val leading = (firstDow.value - DayOfWeek.MONDAY.value + 7) % 7
        val cells: List<CalendarDay?> = buildList {
            repeat(leading) { add(null) }
            addAll(state.calendarDays)
            while (size % 7 != 0) add(null)
        }
        cells.chunked(7).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                row.forEach { cell ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        if (cell != null) CalendarCell(cell)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Legend
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Worst rating per day", modifier = Modifier.weight(1f),
            color = palette.fgMuted, fontSize = 11.sp)
        (1..5).forEach { n ->
            val c = RatingColors.getValue(n)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(c.bg))
                Text("$n", color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CalendarCell(cell: CalendarDay) {
    val palette = LocalAppPalette.current
    val rc = cell.worstRating?.let { RatingColors[it] }
    val bg: Color = when {
        cell.isFuture -> Color.Transparent
        rc != null -> rc.bg
        else -> MaterialTheme.colorScheme.surface
    }
    val fg: Color = when {
        cell.isFuture -> palette.fgDisabled
        rc != null -> rc.fg
        else -> palette.fgMuted
    }
    val borderColor =
        if (cell.isToday) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(11.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${cell.date.dayOfMonth}", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (cell.poopCount > 0) {
            Text("${cell.poopCount}×", color = fg.copy(alpha = 0.7f),
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = palette.fgMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TrendsView(state: HistoryUiState, viewModel: HistoryViewModel) {
    val palette = LocalAppPalette.current

    // Range picker
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TrendsRange.entries.forEach { r ->
            val isOn = r == state.range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isOn) palette.surfaceHigh else Color.Transparent)
                    .clickable { viewModel.setRange(r) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(r.label,
                    color = if (isOn) MaterialTheme.colorScheme.onSurface else palette.fgMuted,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Blood rating chart
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("BLOOD RATING", color = palette.fgMuted,
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        val avg = state.ratingAvg
        Row(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = avg?.let { "%.1f".format(it) } ?: "—",
                fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface)
            Text("avg", color = palette.fgMuted, fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        RatingLineChart(state.ratingPoints, state.rangeStart, state.rangeEnd)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Frequency bars
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("POOPS PER DAY", color = palette.fgMuted,
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Row(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("%.1f".format(state.frequencyAvg),
                fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface)
            Text("avg / day", color = palette.fgMuted, fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        FrequencyBarChart(state.frequencyBars, state.rangeStart, state.rangeEnd)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Food suspects placeholder
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("FOOD SUSPECTS", color = palette.fgMuted,
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Foods eaten in the 24h before a rating ≥ 3, ranked by how much they nudged the rating up or down. Needs more data — keep logging.",
            color = palette.fgMuted, fontSize = 12.sp, lineHeight = 17.sp,
        )
    }
}

@Composable
private fun RatingLineChart(
    points: List<Pair<LocalDate, Int>>,
    rangeStart: LocalDate?,
    rangeEnd: LocalDate,
) {
    val palette = LocalAppPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = palette.borderSubtle
    val chartHeight = 128.dp

    Row(modifier = Modifier.fillMaxWidth()) {
        // Y axis: rating scale 5 (top) → 1 (bottom), aligned with the grid lines.
        YAxisLabels(
            labels = listOf("5", "4", "3", "2", "1"),
            height = chartHeight,
            topPad = 4.dp,
            bottomPad = 12.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                val pad = 6.dp.toPx()
                val w = size.width
                val h = size.height - 8.dp.toPx()
                val plotW = w - pad * 2
                // grid lines for 1..5
                for (n in 1..5) {
                    val y = pad + (1f - (n - 1) / 4f) * (h - pad * 2)
                    drawLine(gridColor, Offset(pad, y), Offset(w - pad, y), strokeWidth = 1.dp.toPx())
                }
                if (points.isEmpty() || rangeStart == null) return@Canvas
                val totalDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd).toFloat().coerceAtLeast(1f)
                fun xFor(d: LocalDate): Float {
                    val frac = (ChronoUnit.DAYS.between(rangeStart, d).toFloat() / totalDays).coerceIn(0f, 1f)
                    return pad + frac * plotW
                }
                fun yFor(r: Int): Float = pad + (1f - (r - 1) / 4f) * (h - pad * 2)
                val path = Path()
                points.forEachIndexed { i, (d, r) ->
                    if (i == 0) path.moveTo(xFor(d), yFor(r)) else path.lineTo(xFor(d), yFor(r))
                }
                drawPath(path, color = primary, style = Stroke(width = 2.2.dp.toPx()))
                points.forEach { (d, r) ->
                    val rc = RatingColors[r]
                    if (rc != null) drawCircle(rc.bg, radius = 3.dp.toPx(), center = Offset(xFor(d), yFor(r)))
                }
            }
            XAxisLabels(rangeStart, rangeEnd)
        }
    }
}

@Composable
private fun FrequencyBarChart(
    bars: List<Pair<LocalDate, Int>>,
    rangeStart: LocalDate?,
    rangeEnd: LocalDate,
) {
    val palette = LocalAppPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val danger = Color(0xFFF97316)
    val chartHeight = 70.dp
    val maxV = (bars.maxOfOrNull { it.second } ?: 0).coerceAtLeast(5)

    Row(modifier = Modifier.fillMaxWidth()) {
        // Y axis: poops-per-day count, max (top) → 0 (bottom).
        YAxisLabels(
            labels = listOf("$maxV", "0"),
            height = chartHeight,
            topPad = 0.dp,
            bottomPad = 0.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                if (bars.isEmpty()) return@Canvas
                val gap = 2.dp.toPx()
                val w = size.width
                val h = size.height
                val barW = (w - gap * (bars.size - 1)) / bars.size
                val maxVf = maxV.toFloat()
                bars.forEachIndexed { i, (_, v) ->
                    val x = i * (barW + gap)
                    val barH = if (v == 0) 2.dp.toPx() else (v / maxVf) * h
                    val color = if (v >= 3) danger else primary
                    drawRect(color = color, topLeft = Offset(x, h - barH),
                        size = androidx.compose.ui.geometry.Size(barW, barH))
                }
            }
            XAxisLabels(rangeStart, rangeEnd)
        }
    }
}

/** Vertical tick labels for a chart's Y axis, evenly spaced top → bottom. */
@Composable
private fun YAxisLabels(
    labels: List<String>,
    height: Dp,
    topPad: Dp,
    bottomPad: Dp,
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .width(18.dp)
            .height(height)
            .padding(top = topPad, bottom = bottomPad, end = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        labels.forEach { label ->
            Text(label, color = palette.fgFaint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Start-of-range and "today" tick labels for a chart's X (time) axis. */
@Composable
private fun XAxisLabels(rangeStart: LocalDate?, rangeEnd: LocalDate) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 6.dp, end = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(rangeStart?.format(axisDateFmt) ?: "—",
            color = palette.fgFaint, fontSize = 9.sp)
        Text("Today · ${rangeEnd.format(axisDateFmt)}",
            color = palette.fgFaint, fontSize = 9.sp)
    }
}
