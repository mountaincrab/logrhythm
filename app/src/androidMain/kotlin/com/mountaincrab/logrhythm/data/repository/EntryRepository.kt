package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.ExtrasTagDao
import com.mountaincrab.logrhythm.data.local.dao.FoodEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteEntryDao
import com.mountaincrab.logrhythm.data.local.dao.PoopEntryDao
import com.mountaincrab.logrhythm.data.local.dao.StoolTagDao
import com.mountaincrab.logrhythm.data.local.entity.ExtrasTagEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryStoolTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.StoolTagEntity
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
    data class Poop(val entity: PoopEntryEntity, val tags: List<StoolTagEntity> = emptyList()) : TimelineEntry(entity.id, entity.occurredAt)
    data class Food(val entity: FoodEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
    data class Note(val entity: NoteEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
}

class EntryRepository(
    private val poopDao: PoopEntryDao,
    private val foodDao: FoodEntryDao,
    private val noteDao: NoteEntryDao,
    private val stoolTagDao: StoolTagDao,
    private val extrasTagDao: ExtrasTagDao,
) {

    fun observeTimeline(): Flow<List<TimelineEntry>> {
        val tagsFlow = combine(
            stoolTagDao.observeAll(),
            stoolTagDao.observeAllCrossRefs(),
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

    fun observeAllStoolTags(): Flow<List<StoolTagEntity>> = stoolTagDao.observeAll()

    suspend fun getPoopTags(entryId: String): List<StoolTagEntity> =
        stoolTagDao.getTagsForEntry(entryId)

    suspend fun createStoolTag(name: String): StoolTagEntity {
        val tag = StoolTagEntity(name = name.trim())
        stoolTagDao.upsert(tag)
        return tag
    }

    suspend fun deleteStoolTag(id: String) = stoolTagDao.softDelete(id)

    fun observeAllExtrasTags(): Flow<List<ExtrasTagEntity>> = extrasTagDao.observeAll()

    suspend fun getNoteExtrasTags(entryId: String): List<ExtrasTagEntity> =
        extrasTagDao.getTagsForEntry(entryId)

    suspend fun createExtrasTag(name: String): ExtrasTagEntity {
        val tag = ExtrasTagEntity(name = name.trim())
        extrasTagDao.upsert(tag)
        return tag
    }

    suspend fun deleteExtrasTag(id: String) = extrasTagDao.softDelete(id)

    suspend fun savePoop(
        id: String? = null,
        occurredAt: Long,
        bristolTypes: Set<Int>,
        blood: Int,
        notes: String?,
        stoolTagIds: Set<String> = emptySet(),
    ) {
        val now = currentTimeMillis()
        val existing = id?.let { poopDao.getById(it) }
        val entry = existing?.copy(
            occurredAt = occurredAt,
            bristolTypes = bristolTypes,
            blood = blood,
            notes = notes?.takeIf { it.isNotBlank() },
            updatedAt = now,
        ) ?: PoopEntryEntity(
            occurredAt = occurredAt,
            bristolTypes = bristolTypes,
            blood = blood,
            notes = notes?.takeIf { it.isNotBlank() },
        )
        poopDao.upsert(entry)
        stoolTagDao.replaceTagsForEntry(entry.id, stoolTagIds.toList())
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
        ) ?: FoodEntryEntity(
            occurredAt = occurredAt,
            items = items,
            mealTag = mealTag,
        )
        foodDao.upsert(entry)
    }

    suspend fun saveNote(
        id: String? = null,
        occurredAt: Long,
        content: String,
        caffeine: Boolean = false,
        alcohol: Boolean = false,
        extrasTagIds: Set<String> = emptySet(),
    ) {
        val now = currentTimeMillis()
        val existing = id?.let { noteDao.getById(it) }
        val entry = existing?.copy(
            occurredAt = occurredAt,
            content = content,
            caffeine = caffeine,
            alcohol = alcohol,
            updatedAt = now,
        ) ?: NoteEntryEntity(
            occurredAt = occurredAt,
            content = content,
            caffeine = caffeine,
            alcohol = alcohol,
        )
        noteDao.upsert(entry)
        extrasTagDao.replaceTagsForEntry(entry.id, extrasTagIds.toList())
    }

    suspend fun deletePoop(id: String) = poopDao.softDelete(id)
    suspend fun deleteFood(id: String) = foodDao.softDelete(id)
    suspend fun deleteNote(id: String) = noteDao.softDelete(id)
}
