package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.DoseStatus
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

/**
 * A recorded dose — a real row on the timeline, exactly like a poop / food / note entry.
 *
 * Rows come from two places:
 *  - materialisation, once a scheduled dose's time has passed ([DoseStatus.SCHEDULED],
 *    [scheduleId] set, id derived from schedule + date so both devices converge on one doc);
 *  - the user logging a one-off ([DoseStatus.MANUAL], no [scheduleId]).
 *
 * [medicationName] is a snapshot so a dose still reads correctly if its catalog entry is
 * later deleted; the live catalog name wins whenever the medication still exists.
 */
@Entity(tableName = "medication_entries")
data class MedicationEntryEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val profileId: String = DEFAULT_PROFILE_ID,
    val medicationId: String,
    val medicationName: String,
    val occurredAt: Long,
    val amount: String = "",
    val unit: String = "",
    val status: DoseStatus = DoseStatus.MANUAL,
    /** The scheduled dose this materialised from, or null for a manually logged dose. */
    val scheduleId: String? = null,
    val notes: String? = null,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
) {
    companion object {
        /**
         * Deterministic id for a materialised dose. Both surfaces compute the same value,
         * so a dose written concurrently on phone and web converges to one document
         * instead of duplicating.
         */
        fun materialisedId(scheduleId: String, isoDate: String): String = "${scheduleId}_$isoDate"
    }
}
