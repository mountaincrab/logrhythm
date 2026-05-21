package com.mountaincrab.logrhythm.sync

import android.content.Context

class SyncScheduler(private val context: Context) {
    fun enqueue() = SyncWorker.enqueue(context)
}
