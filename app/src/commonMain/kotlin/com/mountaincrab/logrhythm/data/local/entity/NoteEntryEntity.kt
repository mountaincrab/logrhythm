package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

@Entity(tableName = "note_entries")
data class NoteEntryEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val occurredAt: Long,
    val content: String,
    val caffeine: Boolean = false,
    val alcohol: Boolean = false,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
)
