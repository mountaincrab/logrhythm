package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedComponentDao {
    @Query("SELECT * FROM tracked_components WHERE profileId = :profileId AND isArchived = 0 ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(profileId: String): Flow<List<TrackedComponentEntity>>

    @Query("SELECT * FROM tracked_components WHERE profileId = :profileId AND isArchived = 1 ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeArchived(profileId: String): Flow<List<TrackedComponentEntity>>

    @Query("SELECT * FROM tracked_components WHERE profileId = :profileId ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeForLookup(profileId: String): Flow<List<TrackedComponentEntity>>

    @Query("SELECT * FROM tracked_components WHERE profileId = :profileId ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getAllForLookup(profileId: String): List<TrackedComponentEntity>

    @Query("SELECT * FROM tracked_components WHERE id = :id")
    suspend fun getById(id: String): TrackedComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(component: TrackedComponentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(components: List<TrackedComponentEntity>)

    @Query("""
        SELECT CASE WHEN EXISTS(SELECT 1 FROM food_item_components WHERE componentId = :id)
          OR EXISTS(SELECT 1 FROM food_entry_line_components WHERE componentId = :id)
        THEN 1 ELSE 0 END
    """)
    suspend fun isUnitLocked(id: String): Boolean

    @Query("UPDATE tracked_components SET isArchived = :archived, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE tracked_components SET isArchived = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isArchived = 0")
    suspend fun archiveByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM tracked_components WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<TrackedComponentEntity>

    @Query("UPDATE tracked_components SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
