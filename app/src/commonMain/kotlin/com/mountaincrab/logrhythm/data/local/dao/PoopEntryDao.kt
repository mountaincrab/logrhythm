package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface PoopEntryDao {
    @Query("SELECT * FROM poop_entries WHERE isDeleted = 0 AND profileId = :profileId ORDER BY occurredAt DESC")
    fun observeAll(profileId: String): Flow<List<PoopEntryEntity>>

    @Query("SELECT * FROM poop_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun observeSince(profileId: String, sinceMillis: Long): Flow<List<PoopEntryEntity>>

    @Query("SELECT * FROM poop_entries WHERE id = :id")
    suspend fun getById(id: String): PoopEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PoopEntryEntity)

    @Query("UPDATE poop_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE poop_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isDeleted = 0")
    suspend fun softDeleteByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM poop_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<PoopEntryEntity>

    @Query("UPDATE poop_entries SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
