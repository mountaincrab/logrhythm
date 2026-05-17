package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "poop_entry_stool_tags",
    primaryKeys = ["entryId", "tagId"],
)
data class PoopEntryStoolTagCrossRef(
    val entryId: String,
    val tagId: String,
)
