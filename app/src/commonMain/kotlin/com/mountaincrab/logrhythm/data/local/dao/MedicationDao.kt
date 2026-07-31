package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isDeleted = 0 AND profileId = :profileId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(profileId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isDeleted = 0 AND profileId = :profileId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(profileId: String): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(medication: MedicationEntity)

    @Query("UPDATE medications SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE medications SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isDeleted = 0")
    suspend fun softDeleteByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM medications WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<MedicationEntity>

    @Query("UPDATE medications SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
