package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.FoodEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteTagDao
import com.mountaincrab.logrhythm.data.local.dao.PoopEntryDao
import com.mountaincrab.logrhythm.data.local.dao.PoopTagDao
import com.mountaincrab.logrhythm.data.local.dao.TimelineDao
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Unified timeline view combining poop / food / note entries.
 * Sorted by occurredAt descending (most recent first), matching the V2 home design.
 */
sealed class TimelineEntry(open val id: String, open val occurredAt: Long) {
    data class Poop(val entity: PoopEntryEntity, val tags: List<PoopTagEntity> = emptyList()) : TimelineEntry(entity.id, entity.occurredAt)
    data class Food(val entity: FoodEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
    data class Note(val entity: NoteEntryEntity, val tags: List<NoteTagEntity> = emptyList()) : TimelineEntry(entity.id, entity.occurredAt)
}

@OptIn(ExperimentalCoroutinesApi::class)
class EntryRepository(
    private val poopDao: PoopEntryDao,
    private val foodDao: FoodEntryDao,
    private val noteDao: NoteEntryDao,
    private val poopTagDao: PoopTagDao,
    private val noteTagDao: NoteTagDao,
    private val timelineDao: TimelineDao,
    private val syncScheduler: com.mountaincrab.logrhythm.sync.SyncScheduler,
    private val activeProfileId: StateFlow<String>,
    private val getUserId: () -> String,
) {

    private fun profileId(): String = activeProfileId.value

    /** Full timeline (all entries) for the active profile. */
    fun observeTimeline(): Flow<List<TimelineEntry>> =
        activeProfileId.flatMapLatest { pid -> timelineWindow(pid, Long.MIN_VALUE) }

    /**
     * Reactive timeline window for the active profile, showing only entries with
     * `occurredAt >= watermark`. The [watermark] flow is the paging cursor: lowering it
     * (via [timelineWatermark]) grows the loaded range one page older, while everything
     * already inside the window stays live (new entries, edits and sync pulls all re-emit).
     */
    fun observeTimeline(watermark: Flow<Long>): Flow<List<TimelineEntry>> =
        combine(activeProfileId, watermark) { pid, since -> pid to since }
            .flatMapLatest { (pid, since) -> timelineWindow(pid, since) }

    private fun timelineWindow(pid: String, sinceMillis: Long): Flow<List<TimelineEntry>> {
        val poopTagsFlow = combine(
            poopTagDao.observeAll(pid),
            poopTagDao.observeAllCrossRefs(),
        ) { tags, refs ->
            val tagMap = tags.associateBy { it.id }
            refs.groupBy { it.entryId }.mapValues { (_, r) -> r.mapNotNull { tagMap[it.tagId] } }
        }
        val noteTagsFlow = combine(
            noteTagDao.observeAll(pid),
            noteTagDao.observeAllCrossRefs(),
        ) { tags, refs ->
            val tagMap = tags.associateBy { it.id }
            refs.groupBy { it.entryId }.mapValues { (_, r) -> r.mapNotNull { tagMap[it.tagId] } }
        }
        return combine(
            poopDao.observeSince(pid, sinceMillis),
            foodDao.observeSince(pid, sinceMillis),
            noteDao.observeSince(pid, sinceMillis),
            poopTagsFlow,
            noteTagsFlow,
        ) { poops, foods, notes, poopTagMap, noteTagMap ->
            buildList<TimelineEntry> {
                poops.forEach { add(TimelineEntry.Poop(it, poopTagMap[it.id] ?: emptyList())) }
                foods.forEach { add(TimelineEntry.Food(it)) }
                notes.forEach { add(TimelineEntry.Note(it, noteTagMap[it.id] ?: emptyList())) }
            }.sortedByDescending { it.occurredAt }
        }
    }

    /**
     * Keyset seek for the timeline window's next lower bound: the `occurredAt` of the
     * [pageSize]-th entry strictly older than [before] (across all three entry types).
     * Returns [Long.MIN_VALUE] when fewer than [pageSize] older entries remain, i.e. the
     * window should open fully — there is nothing more to page in.
     */
    suspend fun timelineWatermark(before: Long, pageSize: Int): Long {
        val boundaries = timelineDao.pageBoundaries(profileId(), before, pageSize)
        return if (boundaries.size < pageSize) Long.MIN_VALUE else boundaries.last()
    }

    fun observePoops(): Flow<List<PoopEntryEntity>> =
        activeProfileId.flatMapLatest { poopDao.observeAll(it) }
    fun observePoopsSince(sinceMillis: Long): Flow<List<PoopEntryEntity>> =
        activeProfileId.flatMapLatest { poopDao.observeSince(it, sinceMillis) }

    suspend fun getPoop(id: String): PoopEntryEntity? = poopDao.getById(id)
    suspend fun getFood(id: String): FoodEntryEntity? = foodDao.getById(id)
    suspend fun getNote(id: String): NoteEntryEntity? = noteDao.getById(id)

    suspend fun foodsInRange(startMillis: Long, endMillis: Long): List<FoodEntryEntity> =
        foodDao.getInRange(profileId(), startMillis, endMillis)

    suspend fun recentFoodItems(limit: Int = 30): List<String> =
        foodDao.recentItems(profileId(), limit)

    fun observeAllPoopTags(): Flow<List<PoopTagEntity>> =
        activeProfileId.flatMapLatest { poopTagDao.observeAll(it) }

    suspend fun getPoopTags(entryId: String): List<PoopTagEntity> =
        poopTagDao.getTagsForEntry(entryId)

    suspend fun createPoopTag(name: String): PoopTagEntity {
        val tag = PoopTagEntity(profileId = profileId(), name = name.trim())
        poopTagDao.upsert(tag)
        syncScheduler.enqueue()
        return tag
    }

    suspend fun deletePoopTag(id: String) {
        poopTagDao.softDelete(id, currentTimeMillis())
        syncScheduler.enqueue()
    }

    fun observeAllNoteTags(): Flow<List<NoteTagEntity>> =
        activeProfileId.flatMapLatest { noteTagDao.observeAll(it) }

    suspend fun getNoteTags(entryId: String): List<NoteTagEntity> =
        noteTagDao.getTagsForEntry(entryId)

    suspend fun createNoteTag(name: String): NoteTagEntity {
        val tag = NoteTagEntity(profileId = profileId(), name = name.trim())
        noteTagDao.upsert(tag)
        syncScheduler.enqueue()
        return tag
    }

    suspend fun deleteNoteTag(id: String) {
        noteTagDao.softDelete(id, currentTimeMillis())
        syncScheduler.enqueue()
    }

    suspend fun savePoop(
        id: String? = null,
        occurredAt: Long,
        bristolTypes: Set<Int>,
        blood: Int,
        notes: String?,
        poopTagIds: Set<String> = emptySet(),
    ) {
        val now = currentTimeMillis()
        val existing = id?.let { poopDao.getById(it) }
        val entry = existing?.copy(
            occurredAt = occurredAt,
            bristolTypes = bristolTypes,
            blood = blood,
            notes = notes?.takeIf { it.isNotBlank() },
            updatedAt = now,
            syncStatus = com.mountaincrab.logrhythm.data.model.SyncStatus.PENDING,
        ) ?: PoopEntryEntity(
            userId = getUserId(),
            profileId = profileId(),
            occurredAt = occurredAt,
            bristolTypes = bristolTypes,
            blood = blood,
            notes = notes?.takeIf { it.isNotBlank() },
        )
        poopDao.upsert(entry)
        poopTagDao.replaceTagsForEntry(entry.id, poopTagIds.toList())
        syncScheduler.enqueue()
    }

    suspend fun saveFood(
        id: String? = null,
        occurredAt: Long,
        items: String,
        mealTag: MealTag?,
    ) {
        val now = currentTimeMillis()
        val existing = id?.let { foodDao.getById(it) }
        val entry = existing?.copy(
            occurredAt = occurredAt,
            items = items,
            mealTag = mealTag,
            updatedAt = now,
            syncStatus = com.mountaincrab.logrhythm.data.model.SyncStatus.PENDING,
        ) ?: FoodEntryEntity(
            userId = getUserId(),
            profileId = profileId(),
            occurredAt = occurredAt,
            items = items,
            mealTag = mealTag,
        )
        foodDao.upsert(entry)
        syncScheduler.enqueue()
    }

    suspend fun saveNote(
        id: String? = null,
        occurredAt: Long,
        content: String,
        caffeine: Boolean = false,
        alcohol: Boolean = false,
        noteTagIds: Set<String> = emptySet(),
    ) {
        val now = currentTimeMillis()
        val existing = id?.let { noteDao.getById(it) }
        val entry = existing?.copy(
            occurredAt = occurredAt,
            content = content,
            caffeine = caffeine,
            alcohol = alcohol,
            updatedAt = now,
            syncStatus = com.mountaincrab.logrhythm.data.model.SyncStatus.PENDING,
        ) ?: NoteEntryEntity(
            userId = getUserId(),
            profileId = profileId(),
            occurredAt = occurredAt,
            content = content,
            caffeine = caffeine,
            alcohol = alcohol,
        )
        noteDao.upsert(entry)
        noteTagDao.replaceTagsForEntry(entry.id, noteTagIds.toList())
        syncScheduler.enqueue()
    }

    suspend fun deletePoop(id: String) { poopDao.softDelete(id); syncScheduler.enqueue() }
    suspend fun deleteFood(id: String) { foodDao.softDelete(id); syncScheduler.enqueue() }
    suspend fun deleteNote(id: String) { noteDao.softDelete(id); syncScheduler.enqueue() }

    /** Cascade soft-delete of all entries and tags belonging to a profile. */
    suspend fun deleteProfileData(profileId: String) {
        poopDao.softDeleteByProfile(profileId)
        foodDao.softDeleteByProfile(profileId)
        noteDao.softDeleteByProfile(profileId)
        poopTagDao.softDeleteByProfile(profileId)
        noteTagDao.softDeleteByProfile(profileId)
        syncScheduler.enqueue()
    }
}
