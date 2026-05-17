package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "note_entry_extras_tags",
    primaryKeys = ["entryId", "tagId"],
)
data class NoteEntryExtrasTagCrossRef(
    val entryId: String,
    val tagId: String,
)
