package com.mountaincrab.logrhythm.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mountaincrab.logrhythm.data.local.entity.DEFAULT_PROFILE_ID
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.data.model.daysFromMask
import com.mountaincrab.logrhythm.data.model.maskFromDays
import kotlinx.coroutines.tasks.await

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

    suspend fun pushFood(uid: String, entity: FoodEntryEntity) {
        userCol(uid, "food_entries").document(entity.id).set(
            mapOf(
                "userId" to uid,
                "profileId" to entity.profileId,
                "occurredAt" to entity.occurredAt,
                "items" to entity.items,
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
                "caffeine" to entity.caffeine,
                "alcohol" to entity.alcohol,
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

    suspend fun pullFood(uid: String, since: Timestamp): List<FoodEntryEntity> =
        userCol(uid, "food_entries")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    FoodEntryEntity(
                        id = doc.id,
                        userId = uid,
                        profileId = doc.getString("profileId") ?: DEFAULT_PROFILE_ID,
                        occurredAt = doc.getLong("occurredAt") ?: return@mapNotNull null,
                        items = doc.getString("items") ?: return@mapNotNull null,
                        mealTag = doc.getString("mealTag")?.let { runCatching { MealTag.valueOf(it) }.getOrNull() },
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                    )
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
                        caffeine = doc.getBoolean("caffeine") ?: false,
                        alcohol = doc.getBoolean("alcohol") ?: false,
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
                        name = doc.getString("name") ?: return@mapNotNull null,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                        sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                    )
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
                "isDeleted" to med.isDeleted,
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
                "isDeleted" to schedule.isDeleted,
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
                "medicationName" to entry.medicationName,
                "dose" to entry.dose,
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
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
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
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
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
                        medicationName = doc.getString("medicationName") ?: "",
                        dose = doc.getString("dose") ?: "",
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
