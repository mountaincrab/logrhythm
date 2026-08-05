package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

/**
 * A recorded dose — a real row on the timeline, exactly like a poop / food / note entry.
 *
 * Rows come from two places:
 *  - materialisation, once a scheduled dose's time has passed ([scheduleId] set, id derived
 *    from schedule + date so both devices converge on one document);
 *  - the user logging a one-off (no [scheduleId]).
 *
 * There is no "taken/skipped" state: the row existing *is* the record. A dose you didn't
 * take is deleted, and one you took differently has its [quantity] edited — the same two
 * gestures every other entry type uses.
 *
 * [medicationName] and [dose] are snapshots so a recorded dose still reads correctly after
 * its catalog entry is edited or deleted.
 */
@Entity(tableName = "medication_entries")
data class MedicationEntryEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val profileId: String = DEFAULT_PROFILE_ID,
    val medicationId: String,
    val medicationName: String,
    /** Strength of one unit at the time this was recorded, e.g. "1g". */
    val dose: String = "",
    /** How many units were taken, e.g. "2". */
    val quantity: String = "",
    val occurredAt: Long,
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
