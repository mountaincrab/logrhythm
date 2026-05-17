package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryStoolTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.StoolTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoolTagDao {
    @Query("SELECT * FROM stool_tags WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<StoolTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: StoolTagEntity)

    @Query("UPDATE stool_tags SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("""
        SELECT t.* FROM stool_tags t
        INNER JOIN poop_entry_stool_tags ref ON t.id = ref.tagId
        WHERE ref.entryId = :entryId
        ORDER BY t.sortOrder ASC, t.createdAt ASC
    """)
    suspend fun getTagsForEntry(entryId: String): List<StoolTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<PoopEntryStoolTagCrossRef>)

    @Query("DELETE FROM poop_entry_stool_tags WHERE entryId = :entryId")
    suspend fun deleteTagsForEntry(entryId: String)

    @Transaction
    suspend fun replaceTagsForEntry(entryId: String, tagIds: List<String>) {
        deleteTagsForEntry(entryId)
        if (tagIds.isNotEmpty()) {
            insertCrossRefs(tagIds.map { PoopEntryStoolTagCrossRef(entryId, it) })
        }
    }
}
