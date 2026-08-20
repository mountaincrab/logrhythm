package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface PoopTagDao {
    @Query("SELECT * FROM poop_tags WHERE isDeleted = 0 AND profileId = :profileId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(profileId: String): Flow<List<PoopTagEntity>>

    /**
     * Every tag, deleted included — what an entry resolves its tag names through. A deleted
     * tag is only retired from the pickers and the Settings list; the entries that carry it
     * keep showing it, so lookups must never filter [isDeleted] out or those chips would
     * silently vanish from history.
     */
    @Query("SELECT * FROM poop_tags WHERE profileId = :profileId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeForLookup(profileId: String): Flow<List<PoopTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: PoopTagEntity)

    @Query("SELECT * FROM poop_tags WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<PoopTagEntity>

    @Query("UPDATE poop_tags SET syncStatus = 'SYNCED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("UPDATE poop_tags SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("UPDATE poop_tags SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isDeleted = 0")
    suspend fun softDeleteByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    /**
     * One-shot repair for tag documents pushed before `profileId` was part of the Firestore
     * shape. Only non-default tags are re-pushed: their profile is the information Firestore
     * lost, whereas a default-profile tag reads back correctly from the missing-field
     * fallback, so re-pushing it could only overwrite a good remote value with a flattened
     * local one.
     */
    @Query("UPDATE poop_tags SET syncStatus = 'PENDING' WHERE profileId != 'default'")
    suspend fun markNonDefaultProfilePending()

    @Query("""
        SELECT t.* FROM poop_tags t
        INNER JOIN poop_entry_tag_refs ref ON t.id = ref.tagId
        WHERE ref.entryId = :entryId
        ORDER BY t.sortOrder ASC, t.createdAt ASC
    """)
    suspend fun getTagsForEntry(entryId: String): List<PoopTagEntity>

    @Query("SELECT * FROM poop_entry_tag_refs")
    fun observeAllCrossRefs(): Flow<List<PoopEntryTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<PoopEntryTagCrossRef>)

    @Query("DELETE FROM poop_entry_tag_refs WHERE entryId = :entryId")
    suspend fun deleteTagsForEntry(entryId: String)

    @Transaction
    suspend fun replaceTagsForEntry(entryId: String, tagIds: List<String>) {
        deleteTagsForEntry(entryId)
        if (tagIds.isNotEmpty()) {
            insertCrossRefs(tagIds.map { PoopEntryTagCrossRef(entryId, it) })
        }
    }
}
