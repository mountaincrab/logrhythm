package com.mountaincrab.logrhythm.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mountaincrab.logrhythm.data.local.AppDatabase
import com.mountaincrab.logrhythm.data.remote.FirestoreRepository
import com.mountaincrab.logrhythm.data.repository.MedicationRepository
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val db: AppDatabase by inject()
    private val firestoreRepo: FirestoreRepository by inject()
    private val prefs: UserPreferencesRepository by inject()
    private val medicationRepo: MedicationRepository by inject()

    override suspend fun doWork(): Result {
        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()

        return try {
            // Push profiles first so entries/tags reference an existing profile.
            db.profileDao().getPending().forEach { profile ->
                firestoreRepo.pushProfile(uid, profile)
                db.profileDao().markSynced(profile.id)
            }

            // Push tags before entries so Firestore has them when entries reference them.
            db.poopTagDao().getPending().forEach { tag ->
                firestoreRepo.pushPoopTag(uid, tag)
                db.poopTagDao().markSynced(tag.id, System.currentTimeMillis())
            }
            db.noteTagDao().getPending().forEach { tag ->
                firestoreRepo.pushNoteTag(uid, tag)
                db.noteTagDao().markSynced(tag.id, System.currentTimeMillis())
            }

            // Push entries with their current tag associations embedded.
            db.poopEntryDao().getPending().forEach { entry ->
                val tagIds = db.poopTagDao().getTagsForEntry(entry.id).map { it.id }
                firestoreRepo.pushPoop(uid, entry, tagIds)
                db.poopEntryDao().markSynced(entry.id, uid)
            }
            db.foodEntryDao().getPending().forEach { entry ->
                firestoreRepo.pushFood(uid, entry)
                db.foodEntryDao().markSynced(entry.id, uid)
            }
            db.noteEntryDao().getPending().forEach { entry ->
                val tagIds = db.noteTagDao().getTagsForEntry(entry.id).map { it.id }
                firestoreRepo.pushNote(uid, entry, tagIds)
                db.noteEntryDao().markSynced(entry.id, uid)
            }

            // Medications before the schedules and doses that reference them.
            db.medicationDao().getPending().forEach { med ->
                firestoreRepo.pushMedication(uid, med)
                db.medicationDao().markSynced(med.id, uid)
            }
            db.medicationScheduleDao().getPending().forEach { schedule ->
                firestoreRepo.pushMedicationSchedule(uid, schedule)
                db.medicationScheduleDao().markSynced(schedule.id, uid)
            }
            pushMedicationEntries(uid)

            pullRemoteChanges(uid)

            // Materialise only after pulling, so a dose another device already recorded
            // (and possibly marked taken) is seen as existing rather than re-created as
            // SCHEDULED. Anything genuinely new is pushed straight away.
            if (medicationRepo.materialiseDueDoses() > 0) {
                pushMedicationEntries(uid)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun pushMedicationEntries(uid: String) {
        db.medicationEntryDao().getPending().forEach { entry ->
            firestoreRepo.pushMedicationEntry(uid, entry)
            db.medicationEntryDao().markSynced(entry.id, uid)
        }
    }

    private suspend fun pullRemoteChanges(uid: String) {
        val sinceMillis = prefs.getLastSyncTimestamp()
        val since = Timestamp(sinceMillis / 1000, 0)
        // Pull profiles first, then tags, so entries pulled afterwards resolve both.
        firestoreRepo.pullProfile(uid, since).forEach { db.profileDao().upsert(it) }

        firestoreRepo.pullPoopTags(uid, since).forEach { db.poopTagDao().upsert(it) }
        firestoreRepo.pullNoteTags(uid, since).forEach { db.noteTagDao().upsert(it) }

        firestoreRepo.pullPoop(uid, since).forEach { (entity, tagIds) ->
            db.poopEntryDao().upsert(entity)
            db.poopTagDao().replaceTagsForEntry(entity.id, tagIds)
        }
        firestoreRepo.pullFood(uid, since).forEach { db.foodEntryDao().upsert(it) }
        firestoreRepo.pullNote(uid, since).forEach { (entity, tagIds) ->
            db.noteEntryDao().upsert(entity)
            db.noteTagDao().replaceTagsForEntry(entity.id, tagIds)
        }

        // Medications first so schedules and doses resolve against a catalog that exists.
        firestoreRepo.pullMedications(uid, since).forEach { db.medicationDao().upsert(it) }
        firestoreRepo.pullMedicationSchedules(uid, since).forEach { db.medicationScheduleDao().upsert(it) }
        firestoreRepo.pullMedicationEntries(uid, since).forEach { db.medicationEntryDao().upsert(it) }

        prefs.setLastSyncTimestamp(System.currentTimeMillis())
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                SyncScheduler.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }
}
