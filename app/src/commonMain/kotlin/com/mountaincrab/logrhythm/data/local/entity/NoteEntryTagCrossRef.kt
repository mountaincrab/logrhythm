package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "note_entry_tag_refs",
    primaryKeys = ["entryId", "tagId"],
)
data class NoteEntryTagCrossRef(
    val entryId: String,
    val tagId: String,
)
