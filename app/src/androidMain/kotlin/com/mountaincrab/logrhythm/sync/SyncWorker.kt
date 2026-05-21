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

    override suspend fun doWork(): Result {
        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()

        return try {
            db.poopEntryDao().getPending().forEach { entry ->
                firestoreRepo.pushPoop(uid, entry)
                db.poopEntryDao().markSynced(entry.id, uid)
            }
            db.foodEntryDao().getPending().forEach { entry ->
                firestoreRepo.pushFood(uid, entry)
                db.foodEntryDao().markSynced(entry.id, uid)
            }
            db.noteEntryDao().getPending().forEach { entry ->
                firestoreRepo.pushNote(uid, entry)
                db.noteEntryDao().markSynced(entry.id, uid)
            }
            pullRemoteChanges(uid)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun pullRemoteChanges(uid: String) {
        val sinceMillis = prefs.getLastSyncTimestamp()
        val since = Timestamp(sinceMillis / 1000, 0)
        firestoreRepo.pullPoop(uid, since).forEach { db.poopEntryDao().upsert(it) }
        firestoreRepo.pullFood(uid, since).forEach { db.foodEntryDao().upsert(it) }
        firestoreRepo.pullNote(uid, since).forEach { db.noteEntryDao().upsert(it) }
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
