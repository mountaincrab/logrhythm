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
    private val keyNotificationPromptShown = booleanPreferencesKey("notification_prompt_shown")

    private fun quickAddFoodItemIdsKey(profileId: String) =
        stringPreferencesKey("quick_add_food_item_ids:$profileId")

    private fun quickAddWidgetConfigKey(appWidgetId: Int) =
        stringPreferencesKey("quick_add_widget_config:$appWidgetId")

    private fun quickAddWidgetLoggedAtKey(appWidgetId: Int) =
        longPreferencesKey("quick_add_widget_logged_at:$appWidgetId")

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

    /**
     * The shortcut a home-screen widget logs, or `null` while it is unconfigured — which is
     * also what a widget restored onto a wiped device sees, so the widget has to offer setup
     * rather than assume a config exists.
     */
    fun quickAddWidgetConfig(appWidgetId: Int): Flow<QuickAddWidgetConfig?> =
        context.dataStore.data.map { preferences ->
            preferences[quickAddWidgetConfigKey(appWidgetId)]?.let(::decodeQuickAddWidgetConfig)
        }

    suspend fun getQuickAddWidgetConfig(appWidgetId: Int): QuickAddWidgetConfig? =
        quickAddWidgetConfig(appWidgetId).first()

    suspend fun setQuickAddWidgetConfig(appWidgetId: Int, config: QuickAddWidgetConfig) {
        require(config.isValid) { "A quick-add widget needs a profile, a food item and a positive quantity" }
        context.dataStore.edit { preferences ->
            preferences[quickAddWidgetConfigKey(appWidgetId)] = preferencesJson.encodeToString(config)
        }
    }

    /** Drops a removed widget's config and its last-logged stamp so ids are never reused stale. */
    suspend fun clearQuickAddWidget(appWidgetId: Int) {
        context.dataStore.edit { preferences ->
            preferences.remove(quickAddWidgetConfigKey(appWidgetId))
            preferences.remove(quickAddWidgetLoggedAtKey(appWidgetId))
        }
    }

    /**
     * When this widget last logged, shown on the tile itself. It answers "did I already log
     * that tea?" without opening the app, which is the whole point of logging from the
     * launcher, and it is the only feedback a tap gets once the toast has gone.
     */
    fun quickAddWidgetLoggedAt(appWidgetId: Int): Flow<Long?> =
        context.dataStore.data.map { it[quickAddWidgetLoggedAtKey(appWidgetId)] }

    suspend fun setQuickAddWidgetLoggedAt(appWidgetId: Int, millis: Long) {
        context.dataStore.edit { it[quickAddWidgetLoggedAtKey(appWidgetId)] = millis }
    }

    /**
     * Whether the one dialog asking for POST_NOTIFICATIONS has been put up. It exists so the
     * app asks once and then lives with the answer: the permission only buys the quick-add
     * widget's confirmation toast, and a second dialog is what turns a "not now" into a
     * permanent no. See widget/QuickAddNotificationPermission.kt.
     */
    suspend fun isNotificationPromptShown(): Boolean =
        context.dataStore.data.map { it[keyNotificationPromptShown] ?: false }.first()

    suspend fun setNotificationPromptShown() {
        context.dataStore.edit { it[keyNotificationPromptShown] = true }
    }

    private fun decodeQuickAddWidgetConfig(encoded: String): QuickAddWidgetConfig? =
        runCatching { preferencesJson.decodeFromString<QuickAddWidgetConfig>(encoded) }
            .getOrNull()
            ?.takeIf { it.isValid }

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
