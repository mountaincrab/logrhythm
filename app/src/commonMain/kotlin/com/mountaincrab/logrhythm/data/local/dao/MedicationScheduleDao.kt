package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationScheduleDao {
    @Query("SELECT * FROM medication_schedules WHERE isArchived = 0 AND profileId = :profileId ORDER BY timeMinutes ASC")
    fun observeAll(profileId: String): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE isArchived = 1 AND profileId = :profileId ORDER BY timeMinutes ASC")
    fun observeArchived(profileId: String): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE isArchived = 0 AND profileId = :profileId ORDER BY timeMinutes ASC")
    suspend fun getAll(profileId: String): List<MedicationScheduleEntity>

    /**
     * Only live schedules produce doses. Paused ones stay on the Schedule tab; archived ones
     * leave it. Neither materialises.
     */
    @Query("SELECT * FROM medication_schedules WHERE isArchived = 0 AND isActive = 1 AND profileId = :profileId ORDER BY timeMinutes ASC")
    suspend fun getActive(profileId: String): List<MedicationScheduleEntity>

    @Query("SELECT * FROM medication_schedules WHERE id = :id")
    suspend fun getById(id: String): MedicationScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: MedicationScheduleEntity)

    /**
     * Archiving retires a scheduled dose without removing it — a materialised dose's
     * `scheduleId` points here. Restoring goes through the repository, which re-anchors
     * `startEpochDay`.
     */
    @Query("UPDATE medication_schedules SET isArchived = :archived, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long = currentTimeMillis())

    /** Retires every scheduled dose for a medication — used when the medication is archived. */
    @Query("UPDATE medication_schedules SET isArchived = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE medicationId = :medicationId AND isArchived = 0")
    suspend fun archiveByMedication(medicationId: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE medication_schedules SET isArchived = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isArchived = 0")
    suspend fun archiveByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM medication_schedules WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<MedicationScheduleEntity>

    @Query("UPDATE medication_schedules SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
