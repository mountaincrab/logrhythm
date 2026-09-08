package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

/** A quantitative constituent that can be attached to any number of food items. */
@Entity(
    tableName = "tracked_components",
    indices = [Index(value = ["profileId", "isArchived", "sortOrder"])],
)
data class TrackedComponentEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val profileId: String = DEFAULT_PROFILE_ID,
    val name: String,
    /** Canonical unit for every amount of this component, e.g. mg or UK units. */
    val unit: String,
    val sortOrder: Int = 0,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isArchived: Boolean = false,
) {
    companion object {
        fun caffeineId(profileId: String) = "component_${profileId}_caffeine"
        fun alcoholId(profileId: String) = "component_${profileId}_alcohol"
    }
}
