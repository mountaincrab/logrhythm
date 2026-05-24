package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE isDeleted = 0 AND profileId = :profileId ORDER BY occurredAt DESC")
    fun observeAll(profileId: String): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun observeSince(profileId: String, sinceMillis: Long): Flow<List<FoodEntryEntity>>

    @Query("""
        SELECT * FROM food_entries
        WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt BETWEEN :startMillis AND :endMillis
        ORDER BY occurredAt DESC
    """)
    suspend fun getInRange(profileId: String, startMillis: Long, endMillis: Long): List<FoodEntryEntity>

    @Query("SELECT items FROM food_entries WHERE isDeleted = 0 AND profileId = :profileId ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun recentItems(profileId: String, limit: Int): List<String>

    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun getById(id: String): FoodEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FoodEntryEntity)

    @Query("UPDATE food_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE food_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isDeleted = 0")
    suspend fun softDeleteByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM food_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<FoodEntryEntity>

    @Query("UPDATE food_entries SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
