package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationEntryDao {
    @Query("SELECT * FROM medication_entries WHERE isDeleted = 0 AND profileId = :profileId ORDER BY occurredAt DESC")
    fun observeAll(profileId: String): Flow<List<MedicationEntryEntity>>

    @Query("SELECT * FROM medication_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun observeSince(profileId: String, sinceMillis: Long): Flow<List<MedicationEntryEntity>>

    @Query("SELECT * FROM medication_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt BETWEEN :startMillis AND :endMillis ORDER BY occurredAt ASC")
    fun observeInRange(profileId: String, startMillis: Long, endMillis: Long): Flow<List<MedicationEntryEntity>>

    @Query("SELECT * FROM medication_entries WHERE id = :id")
    suspend fun getById(id: String): MedicationEntryEntity?

    /**
     * Ids that already exist for the given window — including soft-deleted ones, so a dose the
     * user deleted is not silently recreated by the next materialisation pass.
     */
    @Query("SELECT id FROM medication_entries WHERE profileId = :profileId AND occurredAt BETWEEN :startMillis AND :endMillis")
    suspend fun existingIdsInRange(profileId: String, startMillis: Long, endMillis: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MedicationEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entries: List<MedicationEntryEntity>)

    @Query("UPDATE medication_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE medication_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isDeleted = 0")
    suspend fun softDeleteByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM medication_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<MedicationEntryEntity>

    @Query("UPDATE medication_entries SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
