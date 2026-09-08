package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mountaincrab.logrhythm.data.local.entity.ComponentContribution
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryLineComponentEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryLineEntity
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE isDeleted = 0 AND profileId = :profileId ORDER BY occurredAt DESC")
    fun observeAll(profileId: String): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun observeSince(profileId: String, sinceMillis: Long): Flow<List<FoodEntryEntity>>

    @Query("""
        SELECT l.* FROM food_entry_lines l
        INNER JOIN food_entries e ON e.id = l.entryId
        WHERE e.isDeleted = 0 AND e.profileId = :profileId AND e.occurredAt >= :sinceMillis
        ORDER BY e.occurredAt DESC, l.position ASC
    """)
    fun observeLinesSince(profileId: String, sinceMillis: Long): Flow<List<FoodEntryLineEntity>>

    @Query("""
        SELECT lc.* FROM food_entry_line_components lc
        INNER JOIN food_entry_lines l ON l.id = lc.lineId
        INNER JOIN food_entries e ON e.id = l.entryId
        WHERE e.isDeleted = 0 AND e.profileId = :profileId AND e.occurredAt >= :sinceMillis
    """)
    fun observeLineComponentsSince(profileId: String, sinceMillis: Long): Flow<List<FoodEntryLineComponentEntity>>

    @Query("""
        SELECT * FROM food_entries
        WHERE isDeleted = 0 AND profileId = :profileId
          AND occurredAt >= :startMillis AND occurredAt < :endMillis
        ORDER BY occurredAt DESC
    """)
    suspend fun getInRange(profileId: String, startMillis: Long, endMillis: Long): List<FoodEntryEntity>

    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun getById(id: String): FoodEntryEntity?

    @Query("SELECT * FROM food_entry_lines WHERE entryId = :entryId ORDER BY position ASC")
    suspend fun getLines(entryId: String): List<FoodEntryLineEntity>

    @Query("""
        SELECT lc.* FROM food_entry_line_components lc
        INNER JOIN food_entry_lines l ON l.id = lc.lineId
        WHERE l.entryId = :entryId
    """)
    suspend fun getLineComponents(entryId: String): List<FoodEntryLineComponentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FoodEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<FoodEntryLineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineComponents(components: List<FoodEntryLineComponentEntity>)

    @Query("DELETE FROM food_entry_lines WHERE entryId = :entryId")
    suspend fun deleteLines(entryId: String)

    @Transaction
    suspend fun upsertWithLines(
        entry: FoodEntryEntity,
        lines: List<FoodEntryLineEntity>,
        lineComponents: List<FoodEntryLineComponentEntity>,
    ) {
        upsert(entry)
        deleteLines(entry.id)
        if (lines.isNotEmpty()) insertLines(lines)
        if (lineComponents.isNotEmpty()) insertLineComponents(lineComponents)
    }

    @Query("""
        SELECT e.occurredAt AS occurredAt, fic.componentId AS componentId,
               (l.quantity * fic.amount) AS amount
        FROM food_entries e
        INNER JOIN food_entry_lines l ON l.entryId = e.id
        INNER JOIN food_item_components fic ON fic.foodItemId = l.foodItemId
        WHERE e.profileId = :profileId AND e.isDeleted = 0
          AND e.occurredAt >= :startMillis AND e.occurredAt < :endMillis
          AND l.quantity IS NOT NULL
        UNION ALL
        SELECT e.occurredAt AS occurredAt, lc.componentId AS componentId, lc.amount AS amount
        FROM food_entries e
        INNER JOIN food_entry_lines l ON l.entryId = e.id
        INNER JOIN food_entry_line_components lc ON lc.lineId = l.id
        WHERE e.profileId = :profileId AND e.isDeleted = 0
          AND e.occurredAt >= :startMillis AND e.occurredAt < :endMillis
    """)
    suspend fun componentContributions(
        profileId: String,
        startMillis: Long,
        endMillis: Long,
    ): List<ComponentContribution>

    @Query("UPDATE food_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE food_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE profileId = :profileId AND isDeleted = 0")
    suspend fun softDeleteByProfile(profileId: String, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM food_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<FoodEntryEntity>

    @Query("UPDATE food_entries SET syncStatus = 'SYNCED', userId = :userId WHERE id = :id")
    suspend fun markSynced(id: String, userId: String)
}
