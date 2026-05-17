package com.mountaincrab.logrhythm.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mountaincrab.logrhythm.data.local.ALL_MIGRATIONS
import com.mountaincrab.logrhythm.data.local.AppDatabase
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.ui.addentry.AddFoodViewModel
import com.mountaincrab.logrhythm.ui.addentry.AddNoteViewModel
import com.mountaincrab.logrhythm.ui.addentry.AddPoopViewModel
import com.mountaincrab.logrhythm.ui.detail.EntryDetailViewModel
import com.mountaincrab.logrhythm.ui.history.HistoryViewModel
import com.mountaincrab.logrhythm.ui.home.HomeViewModel
import com.mountaincrab.logrhythm.ui.settings.SettingsViewModel
import com.mountaincrab.logrhythm.ui.theme.ThemeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { UserPreferencesRepository(androidContext()) }

    single {
        Room.databaseBuilder<AppDatabase>(
            context = androidContext(),
            name = "logrhythm_db",
        )
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single { get<AppDatabase>().poopEntryDao() }
    single { get<AppDatabase>().foodEntryDao() }
    single { get<AppDatabase>().noteEntryDao() }
    single { get<AppDatabase>().poopTagDao() }
    single { get<AppDatabase>().noteTagDao() }

    single { EntryRepository(poopDao = get(), foodDao = get(), noteDao = get(), poopTagDao = get(), noteTagDao = get()) }

    viewModel { ThemeViewModel(prefs = get()) }
    viewModel { HomeViewModel(repository = get()) }
    viewModel { HistoryViewModel(repository = get()) }
    viewModel { SettingsViewModel(prefs = get(), repository = get()) }
    viewModel { (entryId: String?) -> AddPoopViewModel(repository = get(), existingId = entryId) }
    viewModel { (entryId: String?) -> AddFoodViewModel(repository = get(), existingId = entryId) }
    viewModel { (entryId: String?) -> AddNoteViewModel(repository = get(), existingId = entryId) }
    viewModel { (kind: String, entryId: String) ->
        EntryDetailViewModel(repository = get(), kind = kind, entryId = entryId)
    }
}
