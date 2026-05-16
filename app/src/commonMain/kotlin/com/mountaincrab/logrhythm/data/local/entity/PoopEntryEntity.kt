package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

@Entity(tableName = "poop_entries")
data class PoopEntryEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val occurredAt: Long,
    val bristol: Int,
    /** Comma-separated selected Bristol type numbers, e.g. "3,5". Falls back to [bristol] when blank. */
    val bristolTypes: String = "",
    /** 1..5; matches phone.jsx RATING_COLORS. 1 = no blood, 5 = loads. */
    val rating: Int,
    val notes: String? = null,
    val medsMissed: Boolean = false,
    val caffeine: Boolean = false,
    val alcohol: Boolean = false,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
)
