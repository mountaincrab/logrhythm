package com.mountaincrab.logrhythm.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
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
}
