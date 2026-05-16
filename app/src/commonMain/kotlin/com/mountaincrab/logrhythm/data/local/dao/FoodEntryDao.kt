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
    @Query("SELECT * FROM food_entries WHERE isDeleted = 0 ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE isDeleted = 0 AND occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun observeSince(sinceMillis: Long): Flow<List<FoodEntryEntity>>

    @Query("""
        SELECT * FROM food_entries
        WHERE isDeleted = 0 AND occurredAt BETWEEN :startMillis AND :endMillis
        ORDER BY occurredAt DESC
    """)
    suspend fun getInRange(startMillis: Long, endMillis: Long): List<FoodEntryEntity>

    @Query("SELECT items FROM food_entries WHERE isDeleted = 0 ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun recentItems(limit: Int): List<String>

    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun getById(id: String): FoodEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FoodEntryEntity)

    @Query("UPDATE food_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())
}
