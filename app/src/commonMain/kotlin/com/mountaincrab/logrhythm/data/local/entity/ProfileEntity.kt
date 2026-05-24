package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

/** Id of the profile created by the v6→v7 migration that owns all pre-profiles data. */
const val DEFAULT_PROFILE_ID = "default"

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String = randomUUID(),
    val name: String,
    /** AppTheme name; stored as String because AppTheme lives in androidMain. */
    val theme: String = "DEEP_NAVY",
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
)
