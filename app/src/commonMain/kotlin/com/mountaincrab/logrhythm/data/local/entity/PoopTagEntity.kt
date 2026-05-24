package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

@Entity(tableName = "poop_tags")
data class PoopTagEntity(
    @PrimaryKey val id: String = randomUUID(),
    @ColumnInfo(defaultValue = "default") val profileId: String = DEFAULT_PROFILE_ID,
    val name: String,
    val isDeleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)
