package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.FoodEntryDao
import com.mountaincrab.logrhythm.data.local.dao.FoodItemDao
import com.mountaincrab.logrhythm.data.local.dao.TrackedComponentDao
import com.mountaincrab.logrhythm.data.local.entity.ComponentContribution
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryLineComponentEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryLineEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryWithLines
import com.mountaincrab.logrhythm.data.local.entity.FoodItemComponentEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodItemWithComponents
import com.mountaincrab.logrhythm.data.local.entity.ResolvedFoodEntryLine
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.sync.SyncScheduler
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class FoodEntryLineInput(
    val id: String = randomUUID(),
    val foodItemId: String? = null,
    val quantity: Double? = null,
    val customText: String? = null,
    /** Direct totals; used only by custom lines. */
    val componentAmounts: Map<String, Double> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class FoodRepository(
    private val entryDao: FoodEntryDao,
    private val itemDao: FoodItemDao,
    private val componentDao: TrackedComponentDao,
    private val syncScheduler: SyncScheduler,
    private val activeProfileId: StateFlow<String>,
    private val getUserId: () -> String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            activeProfileId.collectLatest { ensureDefaultComponents(it) }
        }
    }

    private fun profileId(): String = activeProfileId.value

    fun observeComponents(): Flow<List<TrackedComponentEntity>> =
        activeProfileId.flatMapLatest(componentDao::observeAll)

    fun observeArchivedComponents(): Flow<List<TrackedComponentEntity>> =
        activeProfileId.flatMapLatest(componentDao::observeArchived)

    fun observeComponentsForLookup(): Flow<List<TrackedComponentEntity>> =
        activeProfileId.flatMapLatest(componentDao::observeForLookup)

    fun observeFoodItems(): Flow<List<FoodItemWithComponents>> =
        activeProfileId.flatMapLatest { pid ->
            combine(itemDao.observeAll(pid), itemDao.observeAllComponents()) { items, associations ->
                val byItem = associations.groupBy { it.foodItemId }
                items.map { FoodItemWithComponents(it, byItem[it.id].orEmpty()) }
            }
        }

    fun observeArchivedFoodItems(): Flow<List<FoodItemWithComponents>> =
        activeProfileId.flatMapLatest { pid ->
            combine(itemDao.observeArchived(pid), itemDao.observeAllComponents()) { items, associations ->
                val byItem = associations.groupBy { it.foodItemId }
                items.map { FoodItemWithComponents(it, byItem[it.id].orEmpty()) }
            }
        }

    fun observeFoodItemsForLookup(): Flow<List<FoodItemWithComponents>> =
        activeProfileId.flatMapLatest { pid ->
            combine(itemDao.observeForLookup(pid), itemDao.observeAllComponents()) { items, associations ->
                val byItem = associations.groupBy { it.foodItemId }
                items.map { FoodItemWithComponents(it, byItem[it.id].orEmpty()) }
            }
        }

    fun observeEntries(): Flow<List<FoodEntryWithLines>> =
        activeProfileId.flatMapLatest { resolveEntries(it, Long.MIN_VALUE) }

    fun observeEntriesSince(sinceMillis: Long): Flow<List<FoodEntryWithLines>> =
        activeProfileId.flatMapLatest { resolveEntries(it, sinceMillis) }

    private fun resolveEntries(profileId: String, sinceMillis: Long): Flow<List<FoodEntryWithLines>> =
        combine(
            entryDao.observeSince(profileId, sinceMillis),
            entryDao.observeLinesSince(profileId, sinceMillis),
            entryDao.observeLineComponentsSince(profileId, sinceMillis),
            itemDao.observeForLookup(profileId),
            itemDao.observeAllComponents(),
        ) { entries, lines, customAmounts, items, itemAmounts ->
            val itemMap = items.associateBy { it.id }
            val linesByEntry = lines.groupBy { it.entryId }
            val customByLine = customAmounts.groupBy { it.lineId }
            val amountsByItem = itemAmounts.groupBy { it.foodItemId }
            entries.map { entry ->
                val resolved = linesByEntry[entry.id].orEmpty().sortedBy { it.position }.map { line ->
                    val item = line.foodItemId?.let(itemMap::get)
                    val amounts = if (line.foodItemId != null) {
                        amountsByItem[line.foodItemId].orEmpty().associate { it.componentId to it.amount }
                    } else {
                        customByLine[line.id].orEmpty().associate { it.componentId to it.amount }
                    }
                    ResolvedFoodEntryLine(line, item, amounts)
                }
                FoodEntryWithLines(entry, resolved)
            }
        }

    suspend fun getComponent(id: String): TrackedComponentEntity? = componentDao.getById(id)
    suspend fun isComponentUnitLocked(id: String): Boolean = componentDao.isUnitLocked(id)

    suspend fun saveComponent(id: String? = null, name: String, unit: String): TrackedComponentEntity {
        val trimmedName = name.trim()
        val trimmedUnit = unit.trim()
        require(trimmedName.isNotEmpty() && trimmedUnit.isNotEmpty())
        val pid = profileId()
        val existing = id?.let { componentDao.getById(it) }
        val duplicate = componentDao.getAllForLookup(pid).any {
            it.id != id && !it.isArchived && it.name.equals(trimmedName, ignoreCase = true)
        }
        require(!duplicate) { "A component named $trimmedName already exists" }
        if (existing != null && existing.unit != trimmedUnit) {
            require(!componentDao.isUnitLocked(existing.id)) { "The unit is locked after first use" }
        }
        val now = currentTimeMillis()
        val component = existing?.copy(
            name = trimmedName,
            unit = trimmedUnit,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
        ) ?: TrackedComponentEntity(
            userId = getUserId(),
            profileId = pid,
            name = trimmedName,
            unit = trimmedUnit,
            sortOrder = componentDao.getAllForLookup(pid).size,
        )
        componentDao.upsert(component)
        syncScheduler.enqueue()
        return component
    }

    suspend fun setComponentArchived(id: String, archived: Boolean) {
        componentDao.setArchived(id, archived)
        syncScheduler.enqueue()
    }

    suspend fun getFoodItem(id: String): FoodItemWithComponents? {
        val item = itemDao.getById(id) ?: return null
        return FoodItemWithComponents(item, itemDao.getComponents(id))
    }

    suspend fun saveFoodItem(
        id: String? = null,
        name: String,
        amount: String,
        unit: String,
        componentAmounts: Map<String, Double>,
    ): FoodItemWithComponents {
        val trimmedName = name.trim()
        val trimmedAmount = amount.trim()
        val trimmedUnit = unit.trim()
        require(trimmedName.isNotEmpty() && trimmedAmount.isNotEmpty() && trimmedUnit.isNotEmpty())
        require(componentAmounts.values.all { it.isFinite() && it > 0.0 })
        val pid = profileId()
        val existing = id?.let { itemDao.getById(it) }
        val now = currentTimeMillis()
        val item = existing?.copy(
            name = trimmedName,
            amount = trimmedAmount,
            unit = trimmedUnit,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
        ) ?: FoodItemEntity(
            userId = getUserId(),
            profileId = pid,
            name = trimmedName,
            amount = trimmedAmount,
            unit = trimmedUnit,
            sortOrder = itemDao.getAllForLookup(pid).size,
        )
        val components = componentAmounts.map { (componentId, value) ->
            FoodItemComponentEntity(item.id, componentId, value)
        }
        itemDao.upsertWithComponents(item, components)
        syncScheduler.enqueue()
        return FoodItemWithComponents(item, components)
    }

    suspend fun setFoodItemArchived(id: String, archived: Boolean) {
        itemDao.setArchived(id, archived)
        syncScheduler.enqueue()
    }

    suspend fun getEntry(id: String): FoodEntryWithLines? {
        val entry = entryDao.getById(id) ?: return null
        val itemMap = itemDao.getAllForLookup(entry.profileId).associateBy { it.id }
        val customAmounts = entryDao.getLineComponents(id).groupBy { it.lineId }
        val lines = entryDao.getLines(id).map { line ->
            val item = line.foodItemId?.let(itemMap::get)
            val amounts = if (item != null) {
                itemDao.getComponents(item.id).associate { it.componentId to it.amount }
            } else {
                customAmounts[line.id].orEmpty().associate { it.componentId to it.amount }
            }
            ResolvedFoodEntryLine(line, item, amounts)
        }
        return FoodEntryWithLines(entry, lines)
    }

    suspend fun entriesInRange(startMillis: Long, endMillis: Long): List<FoodEntryWithLines> =
        entryDao.getInRange(profileId(), startMillis, endMillis).mapNotNull { getEntry(it.id) }

    suspend fun saveEntry(
        id: String? = null,
        occurredAt: Long,
        mealTag: MealTag?,
        inputs: List<FoodEntryLineInput>,
    ) {
        require(inputs.isNotEmpty())
        inputs.forEach { input ->
            val catalog = input.foodItemId != null && input.quantity != null && input.quantity.isFinite() && input.quantity > 0.0 && input.customText == null
            val custom = input.foodItemId == null && !input.customText.isNullOrBlank() && input.quantity == null
            require(catalog || custom) { "Each food line must be a saved item or custom text" }
            require(input.componentAmounts.values.all { it.isFinite() && it > 0.0 })
        }
        val existing = id?.let { entryDao.getById(it) }
        val now = currentTimeMillis()
        val entry = existing?.copy(
            occurredAt = occurredAt,
            mealTag = mealTag,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
        ) ?: FoodEntryEntity(
            userId = getUserId(),
            profileId = profileId(),
            occurredAt = occurredAt,
            mealTag = mealTag,
        )
        val lines = inputs.mapIndexed { position, input ->
            FoodEntryLineEntity(
                id = input.id,
                entryId = entry.id,
                position = position,
                foodItemId = input.foodItemId,
                quantity = input.quantity,
                customText = input.customText?.trim(),
            )
        }
        val customComponents = inputs.flatMap { input ->
            if (input.foodItemId != null) emptyList() else input.componentAmounts.map { (componentId, value) ->
                FoodEntryLineComponentEntity(input.id, componentId, value)
            }
        }
        entryDao.upsertWithLines(entry, lines, customComponents)
        syncScheduler.enqueue()
    }

    suspend fun deleteEntry(id: String) {
        entryDao.softDelete(id)
        syncScheduler.enqueue()
    }

    suspend fun componentContributions(startMillis: Long, endMillis: Long): List<ComponentContribution> =
        entryDao.componentContributions(profileId(), startMillis, endMillis)

    suspend fun deleteProfileData(profileId: String) {
        entryDao.softDeleteByProfile(profileId)
        itemDao.archiveByProfile(profileId)
        componentDao.archiveByProfile(profileId)
        syncScheduler.enqueue()
    }

    private suspend fun ensureDefaultComponents(profileId: String) {
        val now = currentTimeMillis()
        componentDao.insertIfAbsent(
            listOf(
                TrackedComponentEntity(
                    id = TrackedComponentEntity.caffeineId(profileId),
                    userId = getUserId(),
                    profileId = profileId,
                    name = "Caffeine",
                    unit = "mg",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now,
                ),
                TrackedComponentEntity(
                    id = TrackedComponentEntity.alcoholId(profileId),
                    userId = getUserId(),
                    profileId = profileId,
                    name = "Alcohol",
                    unit = "UK units",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        )
        syncScheduler.enqueue()
    }
}
