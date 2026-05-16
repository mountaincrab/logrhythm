package com.mountaincrab.logrhythm

import android.app.Application
import com.mountaincrab.logrhythm.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LogRhythmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LogRhythmApplication)
            modules(appModule)
        }
    }
}
