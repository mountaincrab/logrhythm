package com.mountaincrab.logrhythm.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mountaincrab.logrhythm.data.local.entity.DEFAULT_PROFILE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "logrhythm_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val keyAppTheme = stringPreferencesKey("app_theme")
    private val keyActiveProfileId = stringPreferencesKey("active_profile_id")
    private val keyProfileThemeMigrated = booleanPreferencesKey("profile_theme_migrated")
    private val keyLastSyncTimestamp = longPreferencesKey("last_sync_timestamp")

    /** Legacy theme key, read once during the profile theme migration then unused. */
    val appTheme: Flow<String?> = context.dataStore.data.map { it[keyAppTheme] }

    val activeProfileId: Flow<String> =
        context.dataStore.data.map { it[keyActiveProfileId] ?: DEFAULT_PROFILE_ID }

    suspend fun setActiveProfileId(value: String) {
        context.dataStore.edit { it[keyActiveProfileId] = value }
    }

    suspend fun isProfileThemeMigrated(): Boolean =
        context.dataStore.data.map { it[keyProfileThemeMigrated] ?: false }.first()

    suspend fun setProfileThemeMigrated() {
        context.dataStore.edit { it[keyProfileThemeMigrated] = true }
    }

    suspend fun getLastSyncTimestamp(): Long =
        context.dataStore.data.map { it[keyLastSyncTimestamp] ?: 0L }.first()

    suspend fun setLastSyncTimestamp(millis: Long) {
        context.dataStore.edit { it[keyLastSyncTimestamp] = millis }
    }
}
