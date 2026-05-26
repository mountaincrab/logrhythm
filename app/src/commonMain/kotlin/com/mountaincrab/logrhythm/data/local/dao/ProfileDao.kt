package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE isDeleted = 0 ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id AND isDeleted = 0")
    fun observe(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getAll(): List<ProfileEntity>

    /** Count of live (non-deleted) entries and tags owned by a profile across every per-profile table. */
    @Query(
        "SELECT (SELECT COUNT(*) FROM poop_entries WHERE profileId = :profileId AND isDeleted = 0)" +
            " + (SELECT COUNT(*) FROM food_entries WHERE profileId = :profileId AND isDeleted = 0)" +
            " + (SELECT COUNT(*) FROM note_entries WHERE profileId = :profileId AND isDeleted = 0)" +
            " + (SELECT COUNT(*) FROM poop_tags WHERE profileId = :profileId AND isDeleted = 0)" +
            " + (SELECT COUNT(*) FROM note_tags WHERE profileId = :profileId AND isDeleted = 0)"
    )
    suspend fun countDataForProfile(profileId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("UPDATE profiles SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM profiles WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<ProfileEntity>

    @Query("UPDATE profiles SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}
