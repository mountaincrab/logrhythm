package com.mountaincrab.logrhythm.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "logrhythm_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val keyAppTheme = stringPreferencesKey("app_theme")
    private val keyStoolSystem = stringPreferencesKey("stool_type_system")

    val appTheme: Flow<String?> = context.dataStore.data.map { it[keyAppTheme] }

    suspend fun setAppTheme(value: String) {
        context.dataStore.edit { it[keyAppTheme] = value }
    }

    /** "BRISTOL", "PLAIN", or "BOTH" — default BRISTOL. */
    val stoolSystem: Flow<String?> = context.dataStore.data.map { it[keyStoolSystem] }

    suspend fun setStoolSystem(value: String) {
        context.dataStore.edit { it[keyStoolSystem] = value }
    }
}
