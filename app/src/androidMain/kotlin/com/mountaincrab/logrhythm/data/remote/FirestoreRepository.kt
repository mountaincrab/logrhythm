package com.mountaincrab.logrhythm.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mountaincrab.logrhythm.data.local.entity.DEFAULT_PROFILE_ID
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryLineComponentEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryLineEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodItemComponentEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.data.model.daysFromMask
import com.mountaincrab.logrhythm.data.model.maskFromDays
import kotlinx.coroutines.tasks.await

data class RemoteFoodItem(
    val item: FoodItemEntity,
    val components: List<FoodItemComponentEntity>,
)

data class RemoteFoodEntry(
    val entry: FoodEntryEntity,
    val lines: List<FoodEntryLineEntity>,
    val lineComponents: List<FoodEntryLineComponentEntity>,
)

class FirestoreRepository {
    private val db get() = Firebase.firestore

    private fun userCol(uid: String, collection: String) =
        db.collection("users").document(uid).collection(collection)

    suspend fun pushProfile(uid: String, entity: ProfileEntity) {
        userCol(uid, "profiles").document(entity.id).set(
            mapOf(
                "name" to entity.name,
                "theme" to entity.theme,
                "createdAt" to entity.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isDeleted" to entity.isDeleted,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pullProfile(uid: String, since: Timestamp): List<ProfileEntity> =
        userCol(uid, "profiles")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    ProfileEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        theme = doc.getString("theme") ?: "DEEP_NAVY",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                    )
                } catch (_: Exception) { null }
            }

    suspend fun pushPoop(uid: String, entity: PoopEntryEntity, tagIds: List<String>) {
        userCol(uid, "poop_entries").document(entity.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to entity.profileId,
                "occurredAt" to entity.occurredAt,
                "bristolTypes" to entity.bristolTypes.sorted(),
                "blood" to entity.blood,
                "notes" to entity.notes,
                "tagIds" to tagIds,
                "createdAt" to entity.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isDeleted" to entity.isDeleted,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pushFood(
        uid: String,
        entity: FoodEntryEntity,
        lines: List<FoodEntryLineEntity>,
        lineComponents: List<FoodEntryLineComponentEntity>,
    ) {
        val customByLine = lineComponents.groupBy { it.lineId }
        val remoteLines = lines.sortedBy { it.position }.map { line ->
            if (line.foodItemId != null) {
                mapOf(
                    "id" to line.id,
                    "foodItemId" to line.foodItemId,
                    "quantity" to line.quantity,
                )
            } else {
                mapOf(
                    "id" to line.id,
                    "customText" to line.customText,
                    "componentAmounts" to customByLine[line.id].orEmpty().associate { it.componentId to it.amount },
                )
            }
        }
        userCol(uid, "food_entries").document(entity.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to entity.profileId,
                "occurredAt" to entity.occurredAt,
                "schemaVersion" to 2,
                "lines" to remoteLines,
                "items" to FieldValue.delete(),
                "mealTag" to entity.mealTag?.name,
                "createdAt" to entity.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isDeleted" to entity.isDeleted,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pushNote(uid: String, entity: NoteEntryEntity, tagIds: List<String>) {
        userCol(uid, "note_entries").document(entity.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to entity.profileId,
                "occurredAt" to entity.occurredAt,
                "content" to entity.content,
                "caffeine" to FieldValue.delete(),
                "alcohol" to FieldValue.delete(),
                "tagIds" to tagIds,
                "createdAt" to entity.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isDeleted" to entity.isDeleted,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pushPoopTag(uid: String, tag: PoopTagEntity) {
        userCol(uid, "poop_tags").document(tag.id).set(
            mapOf(
                "profileId" to tag.profileId,
                "name" to tag.name,
                "isDeleted" to tag.isDeleted,
                "sortOrder" to tag.sortOrder,
                "createdAt" to tag.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pushNoteTag(uid: String, tag: NoteTagEntity) {
        userCol(uid, "note_tags").document(tag.id).set(
            mapOf(
                "profileId" to tag.profileId,
                "name" to tag.name,
                "isDeleted" to tag.isDeleted,
                "sortOrder" to tag.sortOrder,
                "createdAt" to tag.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pullPoop(uid: String, since: Timestamp): List<Pair<PoopEntryEntity, List<String>>> =
        userCol(uid, "poop_entries")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    val entity = PoopEntryEntity(
                        id = doc.id,
                        userId = uid,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        occurredAt = doc.getLong("occurredAt") ?: return@mapNotNull null,
                        bristolTypes = (doc.get("bristolTypes") as? List<*>)
                            ?.mapNotNull { (it as? Long)?.toInt() }?.toSet() ?: emptySet(),
                        blood = (doc.getLong("blood") ?: 1L).toInt(),
                        notes = doc.getString("notes"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                    )
                    val tagIds = (doc.get("tagIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    Pair(entity, tagIds)
                } catch (_: Exception) { null }
            }

    suspend fun pullFood(uid: String, since: Timestamp): List<RemoteFoodEntry> =
        userCol(uid, "food_entries")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    // Legacy free-text documents are intentionally not migrated.
                    if ((doc.getLong("schemaVersion") ?: 0L) < 2L) return@mapNotNull null
                    val entry = FoodEntryEntity(
                        id = doc.id,
                        userId = uid,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        occurredAt = doc.getLong("occurredAt") ?: return@mapNotNull null,
                        mealTag = doc.getString("mealTag")?.let { runCatching { MealTag.valueOf(it) }.getOrNull() },
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                    )
                    val components = mutableListOf<FoodEntryLineComponentEntity>()
                    val lines = (doc.get("lines") as? List<*>)?.mapIndexedNotNull { position, raw ->
                        val map = raw as? Map<*, *> ?: return@mapIndexedNotNull null
                        val id = map["id"] as? String ?: return@mapIndexedNotNull null
                        val foodItemId = map["foodItemId"] as? String
                        val customText = map["customText"] as? String
                        if (foodItemId != null) {
                            val quantity = (map["quantity"] as? Number)?.toDouble() ?: return@mapIndexedNotNull null
                            FoodEntryLineEntity(id, entry.id, position, foodItemId, quantity, null)
                        } else if (!customText.isNullOrBlank()) {
                            val amountMap = map["componentAmounts"] as? Map<*, *>
                            amountMap.orEmpty().forEach { (componentId, amount) ->
                                if (componentId is String && amount is Number) {
                                    components += FoodEntryLineComponentEntity(id, componentId, amount.toDouble())
                                }
                            }
                            FoodEntryLineEntity(id, entry.id, position, null, null, customText)
                        } else null
                    }.orEmpty()
                    RemoteFoodEntry(entry, lines, components)
                } catch (_: Exception) { null }
            }

    suspend fun pullNote(uid: String, since: Timestamp): List<Pair<NoteEntryEntity, List<String>>> =
        userCol(uid, "note_entries")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    val entity = NoteEntryEntity(
                        id = doc.id,
                        userId = uid,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        occurredAt = doc.getLong("occurredAt") ?: return@mapNotNull null,
                        content = doc.getString("content") ?: return@mapNotNull null,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                    )
                    val tagIds = (doc.get("tagIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    Pair(entity, tagIds)
                } catch (_: Exception) { null }
            }

    suspend fun pullPoopTags(uid: String, since: Timestamp): List<PoopTagEntity> =
        userCol(uid, "poop_tags")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    PoopTagEntity(
                        id = doc.id,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                        sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                    )
                } catch (_: Exception) { null }
            }

    suspend fun pushTrackedComponent(uid: String, component: TrackedComponentEntity) {
        userCol(uid, "tracked_components").document(component.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to component.profileId,
                "name" to component.name,
                "unit" to component.unit,
                "sortOrder" to component.sortOrder,
                "createdAt" to component.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isArchived" to component.isArchived,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pullTrackedComponents(uid: String, since: Timestamp): List<TrackedComponentEntity> =
        userCol(uid, "tracked_components").whereGreaterThan("updatedAt", since).get().await().documents.mapNotNull { doc ->
            try {
                TrackedComponentEntity(
                    id = doc.id,
                    userId = uid,
                    profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    unit = doc.getString("unit") ?: return@mapNotNull null,
                    sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt(),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    syncStatus = SyncStatus.SYNCED,
                    isArchived = doc.getBoolean("isArchived") ?: false,
                )
            } catch (_: Exception) { null }
        }

    suspend fun pushFoodItem(uid: String, item: FoodItemEntity, components: List<FoodItemComponentEntity>) {
        userCol(uid, "food_items").document(item.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to item.profileId,
                "name" to item.name,
                "amount" to item.amount,
                "unit" to item.unit,
                "sortOrder" to item.sortOrder,
                "componentAmounts" to components.associate { it.componentId to it.amount },
                "createdAt" to item.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isArchived" to item.isArchived,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pullFoodItems(uid: String, since: Timestamp): List<RemoteFoodItem> =
        userCol(uid, "food_items").whereGreaterThan("updatedAt", since).get().await().documents.mapNotNull { doc ->
            try {
                val item = FoodItemEntity(
                    id = doc.id,
                    userId = uid,
                    profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    amount = doc.getString("amount") ?: return@mapNotNull null,
                    unit = doc.getString("unit") ?: return@mapNotNull null,
                    sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt(),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    syncStatus = SyncStatus.SYNCED,
                    isArchived = doc.getBoolean("isArchived") ?: false,
                )
                val amounts = (doc.get("componentAmounts") as? Map<*, *>).orEmpty().mapNotNull { (id, value) ->
                    if (id is String && value is Number) FoodItemComponentEntity(item.id, id, value.toDouble()) else null
                }
                RemoteFoodItem(item, amounts)
            } catch (_: Exception) { null }
        }

    suspend fun pushMedication(uid: String, med: MedicationEntity) {
        userCol(uid, "medications").document(med.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to med.profileId,
                "name" to med.name,
                "form" to med.form.name,
                "doseAmount" to med.doseAmount,
                "doseUnit" to med.doseUnit,
                "sortOrder" to med.sortOrder,
                "createdAt" to med.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isArchived" to med.isArchived,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pushMedicationSchedule(uid: String, schedule: MedicationScheduleEntity) {
        userCol(uid, "medication_schedules").document(schedule.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to schedule.profileId,
                "medicationId" to schedule.medicationId,
                "quantity" to schedule.quantity,
                "timeMinutes" to schedule.timeMinutes,
                "repeatRule" to schedule.repeatRule.name,
                // Stored as a sorted ISO day-of-week array (not the Room bitmask), so the
                // document stays readable and the webapp doesn't need the bit layout.
                "daysOfWeek" to daysFromMask(schedule.daysMask),
                "startEpochDay" to schedule.startEpochDay,
                "isActive" to schedule.isActive,
                "createdAt" to schedule.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isArchived" to schedule.isArchived,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pushMedicationEntry(uid: String, entry: MedicationEntryEntity) {
        userCol(uid, "medication_entries").document(entry.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to entry.profileId,
                "medicationId" to entry.medicationId,
                "quantity" to entry.quantity,
                "occurredAt" to entry.occurredAt,
                "scheduleId" to entry.scheduleId,
                "notes" to entry.notes,
                "createdAt" to entry.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isDeleted" to entry.isDeleted,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pullMedications(uid: String, since: Timestamp): List<MedicationEntity> =
        userCol(uid, "medications")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    MedicationEntity(
                        id = doc.id,
                        userId = uid,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        form = MedicationForm.fromName(doc.getString("form")),
                        doseAmount = doc.getString("doseAmount") ?: "",
                        doseUnit = doc.getString("doseUnit") ?: "",
                        sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isArchived = doc.getBoolean("isArchived") ?: false,
                    )
                } catch (_: Exception) { null }
            }

    suspend fun pullMedicationSchedules(uid: String, since: Timestamp): List<MedicationScheduleEntity> =
        userCol(uid, "medication_schedules")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    MedicationScheduleEntity(
                        id = doc.id,
                        userId = uid,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        medicationId = doc.getString("medicationId") ?: return@mapNotNull null,
                        quantity = doc.getString("quantity") ?: "",
                        timeMinutes = (doc.getLong("timeMinutes") ?: return@mapNotNull null).toInt(),
                        repeatRule = RepeatRule.fromName(doc.getString("repeatRule")),
                        daysMask = maskFromDays(
                            (doc.get("daysOfWeek") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList(),
                        ),
                        startEpochDay = doc.getLong("startEpochDay") ?: 0L,
                        isActive = doc.getBoolean("isActive") ?: true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isArchived = doc.getBoolean("isArchived") ?: false,
                    )
                } catch (_: Exception) { null }
            }

    suspend fun pullMedicationEntries(uid: String, since: Timestamp): List<MedicationEntryEntity> =
        userCol(uid, "medication_entries")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    MedicationEntryEntity(
                        id = doc.id,
                        userId = uid,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        medicationId = doc.getString("medicationId") ?: return@mapNotNull null,
                        quantity = doc.getString("quantity") ?: "",
                        occurredAt = doc.getLong("occurredAt") ?: return@mapNotNull null,
                        scheduleId = doc.getString("scheduleId"),
                        notes = doc.getString("notes"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                    )
                } catch (_: Exception) { null }
            }

    suspend fun pullNoteTags(uid: String, since: Timestamp): List<NoteTagEntity> =
        userCol(uid, "note_tags")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    NoteTagEntity(
                        id = doc.id,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                        sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                    )
                } catch (_: Exception) { null }
            }
}
