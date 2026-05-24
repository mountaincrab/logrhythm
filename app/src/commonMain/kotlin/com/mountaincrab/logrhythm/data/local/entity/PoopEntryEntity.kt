package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

@Entity(tableName = "poop_entries")
data class PoopEntryEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    @ColumnInfo(defaultValue = "default") val profileId: String = DEFAULT_PROFILE_ID,
    val occurredAt: Long,
    /** Selected Bristol stool types; stored as a bitmask integer (type N = bit N-1). */
    val bristolTypes: Set<Int> = emptySet(),
    /** 1..5 blood rating; matches phone.jsx RATING_COLORS. 1 = no blood, 5 = loads. */
    val blood: Int,
    val notes: String? = null,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
)
