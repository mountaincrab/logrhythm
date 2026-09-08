package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mountaincrab.logrhythm.data.local.entity.FoodItemComponentEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items WHERE profileId = :profileId AND isArchived = 0 ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(profileId: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE profileId = :profileId AND isArchived = 1 ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeArchived(profileId: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE profileId = :profileId ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeForLookup(profileId: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE profileId = :profileId AND isArchived = 0 ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getAll(profileId: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE profileId = :profileId ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getAllForLookup(profileId: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: String): FoodItemEntity?

    @Query("SELECT * FROM food_item_components")
    fun observeAllComponents(): Flow<List<FoodItemComponentEntity>>

    @Query("SELECT * FROM food_item_components WHERE foodItemId = :foodItemId")
    suspend fun getComponents(foodItemId: String): List<FoodItemComponentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FoodItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<FoodItemComponentEntity>)

    @Query("DELETE FROM food_item_components WHERE foodItemId = :foodItemId")
    suspend fun deleteComponents(foodItemId: String)

    @Transaction
    suspend fun upsertWithComponents(item: FoodItemEntity, components: List<FoodItemComponentEntity>) {
        upsert(item)
        deleteComponents(item.id)
        if (components.isNotEmpty()) insertComponents(components)
    }

    @Query("UPDATE food_items SET isArchived = :archived, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE food_items SET isArchived = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isArchived = 0")
    suspend fun archiveByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM food_items WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<FoodItemEntity>

    @Query("UPDATE food_items SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
