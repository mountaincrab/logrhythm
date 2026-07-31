package com.mountaincrab.logrhythm.data.model

/** Physical form of a medication — drives the icon shown next to it. */
enum class MedicationForm(val label: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    GRANULES("Granules"),
    LIQUID("Liquid"),
    FOAM("Foam"),
    INJECTION("Injection"),
    OTHER("Other");

    /** Rectal/topical forms get their own icon in the design. */
    val isTopical: Boolean get() = this == FOAM

    companion object {
        fun fromName(name: String?): MedicationForm =
            entries.firstOrNull { it.name == name } ?: TABLET
    }
}

/**
 * What actually happened to a dose.
 *
 * [SCHEDULED] is written automatically once a scheduled dose's time has passed: it means
 * "your schedule says this happened", not "you confirmed it". The other states are only ever
 * set by an explicit user action, so the timeline never claims a confirmation you didn't give.
 */
enum class DoseStatus(val label: String) {
    SCHEDULED("As scheduled"),
    TAKEN("Taken"),
    SKIPPED("Skipped"),
    ADJUSTED("Adjusted"),
    MANUAL("Logged");

    /** True for the states the user explicitly chose (vs. auto-materialised). */
    val isConfirmed: Boolean get() = this != SCHEDULED

    companion object {
        fun fromName(name: String?): DoseStatus =
            entries.firstOrNull { it.name == name } ?: SCHEDULED
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

/** "2 g", or just the amount when no unit is set. Blank when neither is set. */
fun formatDose(amount: String, unit: String): String =
    listOf(amount.trim(), unit.trim()).filter { it.isNotEmpty() }.joinToString(" ")
