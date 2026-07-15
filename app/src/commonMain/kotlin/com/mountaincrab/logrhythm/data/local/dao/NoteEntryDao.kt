package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteEntryDao {
    @Query("SELECT * FROM note_entries WHERE isDeleted = 0 AND profileId = :profileId ORDER BY occurredAt DESC")
    fun observeAll(profileId: String): Flow<List<NoteEntryEntity>>

    @Query("SELECT * FROM note_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun observeSince(profileId: String, sinceMillis: Long): Flow<List<NoteEntryEntity>>

    @Query("SELECT * FROM note_entries WHERE id = :id")
    suspend fun getById(id: String): NoteEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: NoteEntryEntity)

    @Query("UPDATE note_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE note_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isDeleted = 0")
    suspend fun softDeleteByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM note_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<NoteEntryEntity>

    @Query("UPDATE note_entries SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
