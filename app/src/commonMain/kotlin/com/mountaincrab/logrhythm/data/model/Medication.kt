package com.mountaincrab.logrhythm.data.model

/** Physical form of a medication — drives the icon shown next to it. */
enum class MedicationForm(val label: String) {
    TABLET("Tablet"),
    GRANULES("Granules"),
    FOAM("Foam"),
    ENEMA("Enema"),
    SUPPOSITORY("Suppository");

    /** Rectal forms get their own icon in the design. */
    val isRectal: Boolean get() = this == FOAM || this == ENEMA || this == SUPPOSITORY

    companion object {
        fun fromName(name: String?): MedicationForm =
            entries.firstOrNull { it.name == name } ?: TABLET
    }
}

/** How often a scheduled dose repeats. */
enum class RepeatRule(val label: String) {
    DAILY("Every day"),
    EVERY_OTHER_DAY("Every other day"),
    WEEKDAYS("Weekdays"),
    SPECIFIC_DAYS("Specific days");

    companion object {
        fun fromName(name: String?): RepeatRule =
            entries.firstOrNull { it.name == name } ?: DAILY
    }
}

/**
 * Coarse bucket a dose falls into, used for grouping and iconography only.
 * Derived from the dose's actual time so there's a single source of truth.
 */
enum class TimeOfDay(val label: String, val defaultMinutes: Int) {
    MORNING("Morning", 8 * 60),
    MIDDAY("Midday", 13 * 60),
    EVENING("Evening", 18 * 60),
    NIGHT("Night", 21 * 60);

    companion object {
        fun forMinutes(minutes: Int): TimeOfDay = when {
            // The small hours belong to the night before, not to the morning.
            minutes < 4 * 60 -> NIGHT
            minutes < 11 * 60 -> MORNING
            minutes < 16 * 60 -> MIDDAY
            minutes < 20 * 60 -> EVENING
            else -> NIGHT
        }
    }
}

/** Bit for an ISO day-of-week (Mon = 1 … Sun = 7) within [MedicationScheduleEntity.daysMask]. */
fun dayOfWeekBit(isoDayOfWeek: Int): Int = 1 shl (isoDayOfWeek - 1)

/** Every ISO day-of-week set in [mask], ascending. */
fun daysFromMask(mask: Int): List<Int> = (1..7).filter { mask and dayOfWeekBit(it) != 0 }

/** Bitmask for [days] (ISO day-of-week values). */
fun maskFromDays(days: Collection<Int>): Int =
    days.fold(0) { acc, d -> acc or dayOfWeekBit(d) }

private val WEEKDAY_MASK: Int = maskFromDays((1..5).toList())

/**
 * Whether a schedule fires on [epochDay].
 *
 * [startEpochDay] is both the "not before" bound and the anchor that gives
 * [RepeatRule.EVERY_OTHER_DAY] a stable parity across devices.
 */
fun scheduleOccursOn(
    rule: RepeatRule,
    daysMask: Int,
    startEpochDay: Long,
    epochDay: Long,
    isoDayOfWeek: Int,
): Boolean {
    if (epochDay < startEpochDay) return false
    return when (rule) {
        RepeatRule.DAILY -> true
        RepeatRule.EVERY_OTHER_DAY -> (epochDay - startEpochDay) % 2 == 0L
        RepeatRule.WEEKDAYS -> WEEKDAY_MASK and dayOfWeekBit(isoDayOfWeek) != 0
        RepeatRule.SPECIFIC_DAYS -> daysMask and dayOfWeekBit(isoDayOfWeek) != 0
    }
}

/** "08:00" for minutes-since-midnight. */
fun formatMinutesOfDay(minutes: Int): String {
    val h = (minutes / 60).coerceIn(0, 23)
    val m = (minutes % 60).coerceIn(0, 59)
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

/**
 * The strength of one unit as a single string, e.g. "1g" from amount "1" + unit "g".
 *
 * Amount and unit are captured separately when defining a medication, but everything that
 * *shows* a strength wants the one string, so they're joined here and nowhere else. Both
 * sides are free text and either may be blank.
 */
fun formatDose(amount: String, unit: String): String = amount.trim() + unit.trim()

/**
 * How much was taken, e.g. "2 × 1g" for two 1g tablets.
 *
 * [dose] is the medication's own strength (defined once, on the catalog entry) and
 * [quantity] is how many of them this dose was. Either side may be blank — a medication
 * without a recorded strength shows just the count.
 */
fun formatDoseAmount(quantity: String, dose: String): String {
    val q = quantity.trim()
    val d = dose.trim()
    return when {
        q.isNotEmpty() && d.isNotEmpty() -> "$q × $d"
        q.isNotEmpty() -> q
        else -> d
    }
}
