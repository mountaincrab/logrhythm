package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "poop_entry_tag_refs",
    primaryKeys = ["entryId", "tagId"],
)
data class PoopEntryTagCrossRef(
    val entryId: String,
    val tagId: String,
)
