package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

/**
 * One scheduled dose: a medication, a quantity, a time of day and a repeat rule.
 *
 * This is the "one dose per line" model — a med taken morning and night is two rows.
 * Because each row carries its [medicationId], the same data renders either as a flat
 * list of doses or grouped per medication; that's a presentation choice, not a schema one.
 *
 * A schedule exists purely to automate adding dose entries. It holds no state about
 * whether a dose actually happened — that lives on the entries it creates.
 */
@Entity(tableName = "medication_schedules")
data class MedicationScheduleEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val profileId: String = DEFAULT_PROFILE_ID,
    val medicationId: String,
    /** How many units of the medication this dose is, e.g. "2" for two 1g tablets. */
    val quantity: String = "",
    /** Minutes since local midnight, e.g. 480 for 08:00. */
    val timeMinutes: Int,
    val repeatRule: RepeatRule = RepeatRule.DAILY,
    /** ISO day-of-week bitmask (Mon = bit 0). Only meaningful for [RepeatRule.SPECIFIC_DAYS]. */
    val daysMask: Int = 0,
    /**
     * Local epoch-day this schedule starts on. Doubles as the parity anchor for
     * [RepeatRule.EVERY_OTHER_DAY] so every device agrees on which days fire, and
     * stops materialisation from back-filling doses from before the schedule existed.
     */
    val startEpochDay: Long,
    /** Paused schedules keep their history but stop producing new doses. */
    val isActive: Boolean = true,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
)
