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
    @Query("SELECT * FROM poop_entries WHERE isDeleted = 0 ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<PoopEntryEntity>>

    @Query("SELECT * FROM poop_entries WHERE isDeleted = 0 AND occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun observeSince(sinceMillis: Long): Flow<List<PoopEntryEntity>>

    @Query("SELECT * FROM poop_entries WHERE id = :id")
    suspend fun getById(id: String): PoopEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PoopEntryEntity)

    @Query("UPDATE poop_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())
}
