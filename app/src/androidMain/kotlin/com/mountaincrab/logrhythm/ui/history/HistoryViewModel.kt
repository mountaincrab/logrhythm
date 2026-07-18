package com.mountaincrab.logrhythm.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.ui.util.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

enum class HistoryTab { CALENDAR, TRENDS }
enum class TrendsRange(val days: Int, val label: String) {
    DAYS_7(7, "7d"),
    DAYS_30(30, "30d"),
    DAYS_90(90, "90d"),
    MONTHS_6(180, "6mo"),
    YEAR_1(365, "1y"),
    ALL(Int.MAX_VALUE, "All"),
}

data class CalendarDay(
    val date: LocalDate,
    val worstRating: Int?,
    val poopCount: Int,
    val isFuture: Boolean,
    val isToday: Boolean,
)

data class HistoryUiState(
    val tab: HistoryTab = HistoryTab.CALENDAR,
    val month: YearMonth = YearMonth.now(),
    val calendarDays: List<CalendarDay> = emptyList(),
    val daysLoggedThisMonth: Int = 0,
    val range: TrendsRange = TrendsRange.DAYS_30,
    // Shared time axis for both trend charts so they line up day-for-day.
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate = LocalDate.now(),
    val ratingPoints: List<Pair<LocalDate, Int>> = emptyList(),
    val ratingAvg: Double? = null,
    val frequencyBars: List<Pair<LocalDate, Int>> = emptyList(),
    val frequencyAvg: Double = 0.0,
)

class HistoryViewModel(repository: EntryRepository) : ViewModel() {

    private val tab = MutableStateFlow(HistoryTab.CALENDAR)
    private val month = MutableStateFlow(YearMonth.now())
    private val range = MutableStateFlow(TrendsRange.DAYS_30)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observePoops(), tab, month, range,
    ) { poops, t, m, r ->
        val today = LocalDate.now()
        val days = buildCalendar(m, poops, today)
        val daysWithEntries = days.count { it.worstRating != null }

        // One shared window drives both charts so their x-axes match.
        val rangeStart = if (r == TrendsRange.ALL) {
            poops.minOfOrNull { it.occurredAt.toLocalDate() }
        } else {
            today.minusDays((r.days - 1).toLong())
        }
        val (ratingPts, ratingAvg) = buildRatingSeries(poops, rangeStart, today)
        val (freqBars, freqAvg) = buildFrequencySeries(poops, rangeStart, today)

        HistoryUiState(
            tab = t,
            month = m,
            calendarDays = days,
            daysLoggedThisMonth = daysWithEntries,
            range = r,
            rangeStart = rangeStart,
            rangeEnd = today,
            ratingPoints = ratingPts,
            ratingAvg = ratingAvg,
            frequencyBars = freqBars,
            frequencyAvg = freqAvg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectTab(t: HistoryTab) = tab.update { t }
    fun setMonth(m: YearMonth) = month.update { m }
    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
    fun setRange(r: TrendsRange) = range.update { r }

    private fun buildCalendar(
        month: YearMonth,
        poops: List<PoopEntryEntity>,
        today: LocalDate,
    ): List<CalendarDay> {
        val ratingsByDay = mutableMapOf<LocalDate, Int>()
        val countByDay = mutableMapOf<LocalDate, Int>()
        poops.forEach { p ->
            val d = p.occurredAt.toLocalDate()
            if (YearMonth.from(d) == month) {
                ratingsByDay[d] = maxOf(ratingsByDay[d] ?: 0, p.blood)
                countByDay[d] = (countByDay[d] ?: 0) + 1
            }
        }
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            CalendarDay(
                date = date,
                worstRating = ratingsByDay[date],
                poopCount = countByDay[date] ?: 0,
                isFuture = date.isAfter(today),
                isToday = date == today,
            )
        }
    }

    private fun buildRatingSeries(
        poops: List<PoopEntryEntity>,
        rangeStart: LocalDate?,
        today: LocalDate,
    ): Pair<List<Pair<LocalDate, Int>>, Double?> {
        val start = rangeStart ?: today
        val byDay = poops
            .filter { it.occurredAt.toLocalDate() in start..today }
            .groupBy { it.occurredAt.toLocalDate() }
            .mapValues { (_, list) -> list.maxOf { it.blood } }
        val points = byDay.toSortedMap().map { (d, r) -> d to r }
        val avg = points.map { it.second }.takeIf { it.isNotEmpty() }?.average()
        return points to avg
    }

    private fun buildFrequencySeries(
        poops: List<PoopEntryEntity>,
        rangeStart: LocalDate?,
        today: LocalDate,
    ): Pair<List<Pair<LocalDate, Int>>, Double> {
        val start = rangeStart ?: today
        val daysList = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .toList()
        val counts = poops.groupingBy { it.occurredAt.toLocalDate() }.eachCount()
        val bars = daysList.map { d -> d to (counts[d] ?: 0) }
        val avg = bars.map { it.second }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        return bars to avg
    }
}
