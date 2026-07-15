package com.mountaincrab.logrhythm.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Cross-table keyset paging support for the unified Home timeline.
 *
 * The three entry tables can't share a single `SELECT *` (their columns differ), but they
 * share the columns we page on, so we union a thin `occurredAt` projection and seek on it.
 * This is a read-only helper over existing tables — no entity/schema change, so no migration.
 */
@Dao
interface TimelineDao {
    /**
     * Returns up to [limit] `occurredAt` values (across poop/food/note) strictly older than
     * [before], newest first. The last value is the lower bound of the next page's window;
     * fewer than [limit] rows means there are no more entries beyond [before].
     */
    @Query(
        """
        SELECT occurredAt FROM (
            SELECT occurredAt FROM poop_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt < :before
            UNION ALL
            SELECT occurredAt FROM food_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt < :before
            UNION ALL
            SELECT occurredAt FROM note_entries WHERE isDeleted = 0 AND profileId = :profileId AND occurredAt < :before
        )
        ORDER BY occurredAt DESC
        LIMIT :limit
    """
    )
    suspend fun pageBoundaries(profileId: String, before: Long, limit: Int): List<Long>
}
