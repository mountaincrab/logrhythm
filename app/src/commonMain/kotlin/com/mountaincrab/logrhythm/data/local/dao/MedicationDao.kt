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
    @Query("SELECT * FROM medications WHERE isArchived = 0 AND profileId = :profileId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(profileId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isArchived = 1 AND profileId = :profileId ORDER BY name COLLATE NOCASE ASC")
    fun observeArchived(profileId: String): Flow<List<MedicationEntity>>

    /**
     * Every medication, archived included — what a recorded dose resolves its name and
     * strength through. Only the pickers filter archived rows out; lookups never do, or a
     * dose of an archived medication would render blank.
     */
    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeForLookup(profileId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isArchived = 0 AND profileId = :profileId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(profileId: String): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAllForLookup(profileId: String): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(medication: MedicationEntity)

    /** Archiving hides a medication from the pickers; it is never removed, so doses still resolve. */
    @Query("UPDATE medications SET isArchived = :archived, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE medications SET isArchived = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isArchived = 0")
    suspend fun archiveByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM medications WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<MedicationEntity>

    @Query("UPDATE medications SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
