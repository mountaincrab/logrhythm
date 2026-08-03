package com.mountaincrab.logrhythm

import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.TimeOfDay
import com.mountaincrab.logrhythm.data.model.daysFromMask
import com.mountaincrab.logrhythm.data.model.formatDoseAmount
import com.mountaincrab.logrhythm.data.model.formatMinutesOfDay
import com.mountaincrab.logrhythm.data.model.maskFromDays
import com.mountaincrab.logrhythm.data.model.scheduleOccursOn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The rules that decide which days a scheduled dose fires on, and the derived id that keeps
 * two devices from creating the same dose twice. Pure logic — no Android, no database.
 */
class MedicationScheduleTest {

    /** Mon 2026-01-05 … a week whose weekday/weekend split is easy to reason about. */
    private val monday = LocalDate.of(2026, 1, 5)

    private fun occurs(
        rule: RepeatRule,
        date: LocalDate,
        start: LocalDate = monday,
        daysMask: Int = 0,
    ) = scheduleOccursOn(
        rule = rule,
        daysMask = daysMask,
        startEpochDay = start.toEpochDay(),
        epochDay = date.toEpochDay(),
        isoDayOfWeek = date.dayOfWeek.value,
    )

    @Test
    fun daily_fires_every_day_from_the_start_date() {
        (0..6).forEach { offset ->
            assertTrue("day +$offset", occurs(RepeatRule.DAILY, monday.plusDays(offset.toLong())))
        }
    }

    /** A schedule must not back-fill doses from before it existed. */
    @Test
    fun nothing_fires_before_the_start_date() {
        assertFalse(occurs(RepeatRule.DAILY, monday.minusDays(1)))
        assertFalse(occurs(RepeatRule.WEEKDAYS, monday.minusDays(3)))
    }

    /**
     * Every-other-day parity is anchored to the start date rather than to "today", so every
     * device agrees on which days fire no matter when it runs.
     */
    @Test
    fun everyOtherDay_alternates_from_its_anchor() {
        assertTrue(occurs(RepeatRule.EVERY_OTHER_DAY, monday))
        assertFalse(occurs(RepeatRule.EVERY_OTHER_DAY, monday.plusDays(1)))
        assertTrue(occurs(RepeatRule.EVERY_OTHER_DAY, monday.plusDays(2)))
        assertFalse(occurs(RepeatRule.EVERY_OTHER_DAY, monday.plusDays(9)))
        assertTrue(occurs(RepeatRule.EVERY_OTHER_DAY, monday.plusDays(10)))
    }

    @Test
    fun weekdays_skips_the_weekend() {
        (0..4).forEach { offset ->
            assertTrue("weekday +$offset", occurs(RepeatRule.WEEKDAYS, monday.plusDays(offset.toLong())))
        }
        assertFalse("saturday", occurs(RepeatRule.WEEKDAYS, monday.plusDays(5)))
        assertFalse("sunday", occurs(RepeatRule.WEEKDAYS, monday.plusDays(6)))
    }

    @Test
    fun specificDays_fires_only_on_the_chosen_days() {
        // Mon + Thu, the "Vitamin D" case from the design.
        val monThu = maskFromDays(listOf(1, 4))
        assertTrue("mon", occurs(RepeatRule.SPECIFIC_DAYS, monday, daysMask = monThu))
        assertFalse("tue", occurs(RepeatRule.SPECIFIC_DAYS, monday.plusDays(1), daysMask = monThu))
        assertTrue("thu", occurs(RepeatRule.SPECIFIC_DAYS, monday.plusDays(3), daysMask = monThu))
        assertFalse("sun", occurs(RepeatRule.SPECIFIC_DAYS, monday.plusDays(6), daysMask = monThu))
    }

    @Test
    fun specificDays_with_no_days_selected_never_fires() {
        (0..6).forEach { offset ->
            assertFalse(occurs(RepeatRule.SPECIFIC_DAYS, monday.plusDays(offset.toLong()), daysMask = 0))
        }
    }

    @Test
    fun dayMask_roundTrips() {
        val days = listOf(1, 3, 7)
        assertEquals(days, daysFromMask(maskFromDays(days)))
        assertEquals(emptyList<Int>(), daysFromMask(0))
    }

    /**
     * The id is what makes materialisation idempotent: the phone and the webapp derive the
     * same value for the same dose, so it converges to one document instead of duplicating.
     */
    @Test
    fun materialisedId_is_derived_from_schedule_and_date() {
        val id = MedicationEntryEntity.materialisedId("sched-1", "2026-01-05")
        assertEquals("sched-1_2026-01-05", id)
        assertEquals(id, MedicationEntryEntity.materialisedId("sched-1", "2026-01-05"))
        assertTrue(id != MedicationEntryEntity.materialisedId("sched-1", "2026-01-06"))
        assertTrue(id != MedicationEntryEntity.materialisedId("sched-2", "2026-01-05"))
    }

    @Test
    fun timeOfDay_buckets_follow_the_clock() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.forMinutes(8 * 60))
        assertEquals(TimeOfDay.MIDDAY, TimeOfDay.forMinutes(13 * 60))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.forMinutes(18 * 60))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.forMinutes(21 * 60))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.forMinutes(0))
    }

    @Test
    fun formatting_helpers_handle_the_empty_cases() {
        assertEquals("08:00", formatMinutesOfDay(8 * 60))
        assertEquals("21:30", formatMinutesOfDay(21 * 60 + 30))
        // Two 1g tablets read as "2 × 1g"; either half may be missing.
        assertEquals("2 × 1g", formatDoseAmount("2", "1g"))
        assertEquals("2", formatDoseAmount("2", ""))
        assertEquals("1g", formatDoseAmount("", "1g"))
        assertEquals("", formatDoseAmount("", ""))
    }
}
