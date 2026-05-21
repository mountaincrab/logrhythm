package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.FoodEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteTagDao
import com.mountaincrab.logrhythm.data.local.dao.PoopEntryDao
import com.mountaincrab.logrhythm.data.local.dao.PoopTagDao
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Unified timeline view combining poop / food / note entries.
 * Sorted by occurredAt descending (most recent first), matching the V2 home design.
 */
sealed class TimelineEntry(open val id: String, open val occurredAt: Long) {
    data class Poop(val entity: PoopEntryEntity, val tags: List<PoopTagEntity> = emptyList()) : TimelineEntry(entity.id, entity.occurredAt)
    data class Food(val entity: FoodEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
    data class Note(val entity: NoteEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
}

class EntryRepository(
    private val poopDao: PoopEntryDao,
    private val foodDao: FoodEntryDao,
    private val noteDao: NoteEntryDao,
    private val poopTagDao: PoopTagDao,
    private val noteTagDao: NoteTagDao,
    private val syncScheduler: com.mountaincrab.logrhythm.sync.SyncScheduler,
    private val getUserId: () -> String,
) {

    fun observeTimeline(): Flow<List<TimelineEntry>> {
        val tagsFlow = combine(
            poopTagDao.observeAll(),
            poopTagDao.observeAllCrossRefs(),
        ) { tags, refs ->
            val tagMap = tags.associateBy { it.id }
            refs.groupBy { it.entryId }.mapValues { (_, r) -> r.mapNotNull { tagMap[it.tagId] } }
        }
        return combine(
            poopDao.observeAll(),
            foodDao.observeAll(),
            noteDao.observeAll(),
            tagsFlow,
        ) { poops, foods, notes, entryTagMap ->
            buildList<TimelineEntry> {
                poops.forEach { add(TimelineEntry.Poop(it, entryTagMap[it.id] ?: emptyList())) }
                foods.forEach { add(TimelineEntry.Food(it)) }
                notes.forEach { add(TimelineEntry.Note(it)) }
            }.sortedByDescending { it.occurredAt }
        }
    }

    fun observePoops(): Flow<List<PoopEntryEntity>> = poopDao.observeAll()
    fun observePoopsSince(sinceMillis: Long): Flow<List<PoopEntryEntity>> = poopDao.observeSince(sinceMillis)

    suspend fun getPoop(id: String): PoopEntryEntity? = poopDao.getById(id)
    suspend fun getFood(id: String): FoodEntryEntity? = foodDao.getById(id)
    suspend fun getNote(id: String): NoteEntryEntity? = noteDao.getById(id)

    suspend fun foodsInRange(startMillis: Long, endMillis: Long): List<FoodEntryEntity> =
        foodDao.getInRange(startMillis, endMillis)

    suspend fun recentFoodItems(limit: Int = 30): List<String> =
        foodDao.recentItems(limit)

    fun observeAllPoopTags(): Flow<List<PoopTagEntity>> = poopTagDao.observeAll()

    suspend fun getPoopTags(entryId: String): List<PoopTagEntity> =
        poopTagDao.getTagsForEntry(entryId)

    suspend fun createPoopTag(name: String): PoopTagEntity {
        val tag = PoopTagEntity(name = name.trim())
        poopTagDao.upsert(tag)
        return tag
    }

    suspend fun deletePoopTag(id: String) = poopTagDao.softDelete(id)

    fun observeAllNoteTags(): Flow<List<NoteTagEntity>> = noteTagDao.observeAll()

    suspend fun getNoteTags(entryId: String): List<NoteTagEntity> =
        noteTagDao.getTagsForEntry(entryId)

    suspend fun createNoteTag(name: String): NoteTagEntity {
        val tag = NoteTagEntity(name = name.trim())
        noteTagDao.upsert(tag)
        return tag
    }

    suspend fun deleteNoteTag(id: String) = noteTagDao.softDelete(id)

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
}
