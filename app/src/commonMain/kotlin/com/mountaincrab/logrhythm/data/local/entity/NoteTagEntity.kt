package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

@Entity(tableName = "note_tags")
data class NoteTagEntity(
    @PrimaryKey val id: String = randomUUID(),
    val name: String,
    val isDeleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = currentTimeMillis(),
)
