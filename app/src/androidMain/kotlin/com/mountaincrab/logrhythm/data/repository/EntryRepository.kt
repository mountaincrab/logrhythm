package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.FoodEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteEntryDao
import com.mountaincrab.logrhythm.data.local.dao.PoopEntryDao
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
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
    data class Poop(val entity: PoopEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
    data class Food(val entity: FoodEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
    data class Note(val entity: NoteEntryEntity) : TimelineEntry(entity.id, entity.occurredAt)
}

class EntryRepository(
    private val poopDao: PoopEntryDao,
    private val foodDao: FoodEntryDao,
    private val noteDao: NoteEntryDao,
) {

    fun observeTimeline(): Flow<List<TimelineEntry>> = combine(
        poopDao.observeAll(),
        foodDao.observeAll(),
        noteDao.observeAll(),
    ) { poops, foods, notes ->
        buildList<TimelineEntry> {
            poops.forEach { add(TimelineEntry.Poop(it)) }
            foods.forEach { add(TimelineEntry.Food(it)) }
            notes.forEach { add(TimelineEntry.Note(it)) }
        }.sortedByDescending { it.occurredAt }
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

    suspend fun savePoop(
        id: String? = null,
        occurredAt: Long,
        bristolTypes: Set<Int>,
        blood: Int,
        notes: String?,
    ) {
        val now = currentTimeMillis()
        val bristolTypesStr = bristolTypes.sorted().joinToString(",")
        val existing = id?.let { poopDao.getById(it) }
        val entry = existing?.copy(
            occurredAt = occurredAt,
            bristolTypes = bristolTypesStr,
            blood = blood,
            notes = notes?.takeIf { it.isNotBlank() },
            updatedAt = now,
        ) ?: PoopEntryEntity(
            occurredAt = occurredAt,
            bristolTypes = bristolTypesStr,
            blood = blood,
            notes = notes?.takeIf { it.isNotBlank() },
        )
        poopDao.upsert(entry)
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
        medsMissed: Boolean = false,
        caffeine: Boolean = false,
        alcohol: Boolean = false,
    ) {
        val now = currentTimeMillis()
        val existing = id?.let { noteDao.getById(it) }
        val entry = existing?.copy(
            occurredAt = occurredAt,
            content = content,
            medsMissed = medsMissed,
            caffeine = caffeine,
            alcohol = alcohol,
            updatedAt = now,
        ) ?: NoteEntryEntity(
            occurredAt = occurredAt,
            content = content,
            medsMissed = medsMissed,
            caffeine = caffeine,
            alcohol = alcohol,
        )
        noteDao.upsert(entry)
    }

    suspend fun deletePoop(id: String) = poopDao.softDelete(id)
    suspend fun deleteFood(id: String) = foodDao.softDelete(id)
    suspend fun deleteNote(id: String) = noteDao.softDelete(id)
}
