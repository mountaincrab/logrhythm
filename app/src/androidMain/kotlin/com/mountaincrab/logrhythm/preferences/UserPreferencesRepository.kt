package com.mountaincrab.logrhythm.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mountaincrab.logrhythm.data.local.entity.DEFAULT_PROFILE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "logrhythm_prefs")

const val MAX_QUICK_ADD_FOOD_ITEMS = 5

private val preferencesJson = Json

enum class HomeTimelineDensity {
    STANDARD,
    COMPACT;

    companion object {
        fun fromName(name: String?): HomeTimelineDensity =
            entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

class UserPreferencesRepository(private val context: Context) {

    private val keyAppTheme = stringPreferencesKey("app_theme")
    private val keyActiveProfileId = stringPreferencesKey("active_profile_id")
    private val keyProfileThemeMigrated = booleanPreferencesKey("profile_theme_migrated")
    private val keyLastSyncTimestamp = longPreferencesKey("last_sync_timestamp")
    private val keyTagProfileIdRepaired = booleanPreferencesKey("tag_profile_id_repaired")
    private val keyDisabledHomeEntryTypes = stringSetPreferencesKey("disabled_home_entry_types")
    private val keyHomeTimelineDensity = stringPreferencesKey("home_timeline_density")
    private val keyGroupHomeByEntryType = booleanPreferencesKey("group_home_by_entry_type")

    private fun quickAddFoodItemIdsKey(profileId: String) =
        stringPreferencesKey("quick_add_food_item_ids:$profileId")

    /** Legacy theme key, read once during the profile theme migration then unused. */
    val appTheme: Flow<String?> = context.dataStore.data.map { it[keyAppTheme] }

    val activeProfileId: Flow<String> =
        context.dataStore.data.map { it[keyActiveProfileId] ?: DEFAULT_PROFILE_ID }

    suspend fun setActiveProfileId(value: String) {
        context.dataStore.edit { it[keyActiveProfileId] = value }
    }

    /** Home timeline entry types hidden on this device. Unknown values are ignored by the UI. */
    val disabledHomeEntryTypes: Flow<Set<String>> =
        context.dataStore.data.map { it[keyDisabledHomeEntryTypes] ?: emptySet() }

    suspend fun toggleHomeEntryType(value: String) {
        context.dataStore.edit { preferences ->
            val disabled = preferences[keyDisabledHomeEntryTypes].orEmpty().toMutableSet()
            if (!disabled.add(value)) disabled.remove(value)
            preferences[keyDisabledHomeEntryTypes] = disabled
        }
    }

    suspend fun clearHomeEntryFilters() {
        context.dataStore.edit { it.remove(keyDisabledHomeEntryTypes) }
    }

    /** Home timeline spacing on this device. It is deliberately not part of profile sync. */
    val homeTimelineDensity: Flow<HomeTimelineDensity> =
        context.dataStore.data.map { HomeTimelineDensity.fromName(it[keyHomeTimelineDensity]) }

    suspend fun setHomeTimelineDensity(value: HomeTimelineDensity) {
        context.dataStore.edit { it[keyHomeTimelineDensity] = value.name }
    }

    /**
     * Whether the home timeline collapses each day into one box per entry type. Like the
     * density above it is a device preference: it changes how the feed is laid out, not what
     * is in it, so it is deliberately not part of profile sync.
     */
    val groupHomeByEntryType: Flow<Boolean> =
        context.dataStore.data.map { it[keyGroupHomeByEntryType] ?: false }

    suspend fun setGroupHomeByEntryType(value: Boolean) {
        context.dataStore.edit { it[keyGroupHomeByEntryType] = value }
    }

    /**
     * Ordered food catalogue ids shown as quick-add shortcuts for [profileId] on this device.
     * `null` means the profile has not configured shortcuts yet, allowing the UI to offer
     * catalogue-order defaults; an empty list is an explicitly cleared configuration.
     */
    fun quickAddFoodItemIds(profileId: String): Flow<List<String>?> =
        context.dataStore.data.map { preferences ->
            preferences[quickAddFoodItemIdsKey(profileId)]?.let { encoded ->
                runCatching { preferencesJson.decodeFromString<List<String>>(encoded) }
                    .getOrDefault(emptyList())
                    .distinct()
                    .take(MAX_QUICK_ADD_FOOD_ITEMS)
            }
        }

    suspend fun setQuickAddFoodItemIds(profileId: String, itemIds: List<String>) {
        val sanitised = itemIds.distinct().take(MAX_QUICK_ADD_FOOD_ITEMS)
        context.dataStore.edit { preferences ->
            preferences[quickAddFoodItemIdsKey(profileId)] = preferencesJson.encodeToString(sanitised)
        }
    }

    suspend fun isProfileThemeMigrated(): Boolean =
        context.dataStore.data.map { it[keyProfileThemeMigrated] ?: false }.first()

    suspend fun setProfileThemeMigrated() {
        context.dataStore.edit { it[keyProfileThemeMigrated] = true }
    }

    /** Whether tags have been re-pushed once to add the `profileId` field to their documents. */
    suspend fun isTagProfileIdRepaired(): Boolean =
        context.dataStore.data.map { it[keyTagProfileIdRepaired] ?: false }.first()

    suspend fun setTagProfileIdRepaired() {
        context.dataStore.edit { it[keyTagProfileIdRepaired] = true }
    }

    suspend fun getLastSyncTimestamp(): Long =
        context.dataStore.data.map { it[keyLastSyncTimestamp] ?: 0L }.first()

    suspend fun setLastSyncTimestamp(millis: Long) {
        context.dataStore.edit { it[keyLastSyncTimestamp] = millis }
    }
}
