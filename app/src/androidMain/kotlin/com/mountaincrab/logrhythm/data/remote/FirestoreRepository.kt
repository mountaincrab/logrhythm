package com.mountaincrab.logrhythm.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.data.model.SyncStatus
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db get() = Firebase.firestore

    private fun userCol(uid: String, collection: String) =
        db.collection("users").document(uid).collection(collection)

    suspend fun pushPoop(uid: String, entity: PoopEntryEntity) {
        userCol(uid, "poop_entries").document(entity.id).set(
            mapOf(
                "userId" to uid,
                "occurredAt" to entity.occurredAt,
                "bristolTypes" to entity.bristolTypes.sorted(),
                "blood" to entity.blood,
                "notes" to entity.notes,
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

    suspend fun pushNote(uid: String, entity: NoteEntryEntity) {
        userCol(uid, "note_entries").document(entity.id).set(
            mapOf(
                "userId" to uid,
                "occurredAt" to entity.occurredAt,
                "content" to entity.content,
                "caffeine" to entity.caffeine,
                "alcohol" to entity.alcohol,
                "createdAt" to entity.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isDeleted" to entity.isDeleted,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun pullPoop(uid: String, since: Timestamp): List<PoopEntryEntity> =
        userCol(uid, "poop_entries")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    PoopEntryEntity(
                        id = doc.id,
                        userId = uid,
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

    suspend fun pullNote(uid: String, since: Timestamp): List<NoteEntryEntity> =
        userCol(uid, "note_entries")
            .whereGreaterThan("updatedAt", since)
            .get().await().documents.mapNotNull { doc ->
                try {
                    NoteEntryEntity(
                        id = doc.id,
                        userId = uid,
                        occurredAt = doc.getLong("occurredAt") ?: return@mapNotNull null,
                        content = doc.getString("content") ?: return@mapNotNull null,
                        caffeine = doc.getBoolean("caffeine") ?: false,
                        alcohol = doc.getBoolean("alcohol") ?: false,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED,
                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                    )
                } catch (_: Exception) { null }
            }
}
