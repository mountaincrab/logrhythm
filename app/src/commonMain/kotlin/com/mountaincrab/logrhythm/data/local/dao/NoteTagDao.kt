package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteTagDao {
    @Query("SELECT * FROM note_tags WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<NoteTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: NoteTagEntity)

    @Query("SELECT * FROM note_tags WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<NoteTagEntity>

    @Query("UPDATE note_tags SET syncStatus = 'SYNCED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("UPDATE note_tags SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("""
        SELECT t.* FROM note_tags t
        INNER JOIN note_entry_tag_refs ref ON t.id = ref.tagId
        WHERE ref.entryId = :entryId
        ORDER BY t.sortOrder ASC, t.createdAt ASC
    """)
    suspend fun getTagsForEntry(entryId: String): List<NoteTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<NoteEntryTagCrossRef>)

    @Query("DELETE FROM note_entry_tag_refs WHERE entryId = :entryId")
    suspend fun deleteTagsForEntry(entryId: String)

    @Transaction
    suspend fun replaceTagsForEntry(entryId: String, tagIds: List<String>) {
        deleteTagsForEntry(entryId)
        if (tagIds.isNotEmpty()) {
            insertCrossRefs(tagIds.map { NoteEntryTagCrossRef(entryId, it) })
        }
    }
}
