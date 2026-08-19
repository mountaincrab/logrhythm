package com.mountaincrab.logrhythm.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.doseUnits
import com.mountaincrab.logrhythm.data.model.formatDose
import com.mountaincrab.logrhythm.data.model.formatMedicationValue
import com.mountaincrab.logrhythm.data.model.parseAmount
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

/**
 * One medication's daily totals over the trends range — the row the Medication card draws.
 *
 * Units are per medication and never summed across them: 4g of one drug and 100mg of
 * another share no axis, so each row is scaled to its own [peak] and carries its own
 * [unit]. [unit] is the medication's own unit ("g", "mg") when its strength is numeric,
 * and "×" when it isn't ("1 puff"), where the totals are counts of units taken.
 */
data class MedicationSeries(
    val medicationId: String,
    val name: String,
    val form: MedicationForm,
    /** The medication's strength as defined, e.g. "1g" — shown next to the name. */
    val strength: String,
    val unit: String,
    /** Stable per-medication colour slot, independent of the range and of which rows show. */
    val colorIndex: Int,
    /** One total per day of the range, oldest first — aligned with [HistoryUiState.frequencyBars]. */
    val dailyTotals: List<Double>,
    val total: Double,
    val avgPerDay: Double,
    val peak: Double,
) {
    /** A value in this medication's unit, e.g. "3.9g". */
    fun format(value: Double, decimals: Int = 2): String =
        formatMedicationValue(value, decimals) + unit
}

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
    val medicationSeries: List<MedicationSeries> = emptyList(),
    /** Whether any medication has ever been defined — an empty catalog gets a different empty line. */
    val hasMedications: Boolean = false,
)

class HistoryViewModel(repository: EntryRepository) : ViewModel() {

    private val tab = MutableStateFlow(HistoryTab.CALENDAR)
    private val month = MutableStateFlow(YearMonth.now())
    private val range = MutableStateFlow(TrendsRange.DAYS_30)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observePoops(), repository.observeDosesWithCatalog(), tab, month, range,
    ) { poops, (doses, catalog), t, m, r ->
        val today = LocalDate.now()
        val days = buildCalendar(m, poops, today)
        val daysWithEntries = days.count { it.worstRating != null }

        // One shared window drives every chart so their x-axes match. "All" reaches back to
        // the oldest entry of either kind — a dose logged before the first poop still counts.
        val rangeStart = if (r == TrendsRange.ALL) {
            listOfNotNull(
                poops.minOfOrNull { it.occurredAt.toLocalDate() },
                doses.minOfOrNull { it.occurredAt.toLocalDate() },
            ).minOrNull()
        } else {
            today.minusDays((r.days - 1).toLong())
        }
        val (ratingPts, ratingAvg) = buildRatingSeries(poops, rangeStart, today)
        val (freqBars, freqAvg) = buildFrequencySeries(poops, rangeStart, today)
        val medSeries = buildMedicationSeries(doses, catalog, rangeStart, today)

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
            medicationSeries = medSeries,
            hasMedications = catalog.isNotEmpty(),
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

    /**
     * Daily totals per medication over the range.
     *
     * Doses carry only a quantity; the strength lives on the catalog row, so a dose is worth
     * `quantity × strength` and every medication is totalled in its own unit. Rows keep the
     * catalog's order — the same order the Meds screen uses — so a medication's colour never
     * moves when the range changes or when another medication drops out of it.
     */
    private fun buildMedicationSeries(
        doses: List<MedicationEntryEntity>,
        catalog: List<MedicationEntity>,
        rangeStart: LocalDate?,
        today: LocalDate,
    ): List<MedicationSeries> {
        if (catalog.isEmpty()) return emptyList()
        val start = rangeStart ?: today
        val days = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .toList()
        if (days.isEmpty()) return emptyList()
        val dayIndex = days.withIndex().associate { (i, d) -> d to i }

        val dosesByMedication = doses
            .filter { it.occurredAt.toLocalDate() in start..today }
            .groupBy { it.medicationId }

        return catalog.mapIndexedNotNull { index, med ->
            val medDoses = dosesByMedication[med.id] ?: return@mapIndexedNotNull null
            val unitAmount = parseAmount(med.doseAmount)
            val totals = DoubleArray(days.size)
            medDoses.forEach { dose ->
                val i = dayIndex[dose.occurredAt.toLocalDate()] ?: return@forEach
                totals[i] += doseUnits(dose.quantity, unitAmount)
            }
            val total = totals.sum()
            if (total <= 0.0) return@mapIndexedNotNull null
            MedicationSeries(
                medicationId = med.id,
                name = med.name,
                form = med.form,
                strength = formatDose(med.doseAmount, med.doseUnit),
                // A non-numeric strength has nothing to multiply by, so the row counts units taken.
                unit = if (unitAmount != null) med.doseUnit.trim() else "×",
                colorIndex = index,
                dailyTotals = totals.toList(),
                total = total,
                avgPerDay = total / days.size,
                peak = totals.maxOrNull() ?: 0.0,
            )
        }
    }
}
