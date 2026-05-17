package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mountaincrab.logrhythm.data.local.entity.ExtrasTagEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryExtrasTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtrasTagDao {
    @Query("SELECT * FROM extras_tags WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<ExtrasTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: ExtrasTagEntity)

    @Query("UPDATE extras_tags SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("""
        SELECT t.* FROM extras_tags t
        INNER JOIN note_entry_extras_tags ref ON t.id = ref.tagId
        WHERE ref.entryId = :entryId
        ORDER BY t.sortOrder ASC, t.createdAt ASC
    """)
    suspend fun getTagsForEntry(entryId: String): List<ExtrasTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<NoteEntryExtrasTagCrossRef>)

    @Query("DELETE FROM note_entry_extras_tags WHERE entryId = :entryId")
    suspend fun deleteTagsForEntry(entryId: String)

    @Transaction
    suspend fun replaceTagsForEntry(entryId: String, tagIds: List<String>) {
        deleteTagsForEntry(entryId)
        if (tagIds.isNotEmpty()) {
            insertCrossRefs(tagIds.map { NoteEntryExtrasTagCrossRef(entryId, it) })
        }
    }
}
