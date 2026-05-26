package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.ProfileDao
import com.mountaincrab.logrhythm.data.local.entity.DEFAULT_PROFILE_ID
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.sync.SyncScheduler
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepository(
    private val dao: ProfileDao,
    private val prefs: UserPreferencesRepository,
    private val syncScheduler: SyncScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Hot snapshot of the active profile id; `.value` is used for synchronous writes. */
    val activeProfileId: StateFlow<String> =
        prefs.activeProfileId.stateIn(scope, SharingStarted.Eagerly, DEFAULT_PROFILE_ID)

    val profiles: Flow<List<ProfileEntity>> = dao.observeAll()

    val activeProfile: Flow<ProfileEntity?> =
        activeProfileId.flatMapLatest { dao.observe(it) }

    init {
        scope.launch {
            ensureDefaultProfile()
            migrateLegacyThemeIfNeeded()
        }
    }

    suspend fun setActiveProfile(id: String) = prefs.setActiveProfileId(id)

    suspend fun createProfile(name: String): ProfileEntity {
        val profile = ProfileEntity(name = name.trim())
        dao.upsert(profile)
        syncScheduler.enqueue()
        return profile
    }

    suspend fun renameProfile(id: String, name: String) {
        dao.getById(id)?.let {
            dao.upsert(it.copy(name = name.trim(), updatedAt = currentTimeMillis(), syncStatus = SyncStatus.PENDING))
            syncScheduler.enqueue()
        }
    }

    /** Soft-deletes the profile row. Caller is responsible for cascading entry/tag deletes. */
    suspend fun deleteProfile(id: String) {
        dao.softDelete(id)
        syncScheduler.enqueue()
    }

    suspend fun setActiveProfileTheme(themeName: String) {
        val id = activeProfileId.value
        dao.getById(id)?.let {
            dao.upsert(it.copy(theme = themeName, updatedAt = currentTimeMillis(), syncStatus = SyncStatus.PENDING))
            syncScheduler.enqueue()
        }
    }

    suspend fun profileCount(): Int = dao.getAll().size

    /**
     * Guarantees a selectable default profile exists. The v8→v9 migration creates one for
     * upgrading users, but fresh installs never run that migration, and a past destructive
     * wipe could strand entries on a "default" profile whose row no longer exists. In either
     * case the active id resolves to nothing (the "?" avatar) and migrated data becomes
     * unreachable. Recreate (or un-delete) the default profile when it's missing and either
     * there are no profiles at all or live data still points at it.
     */
    private suspend fun ensureDefaultProfile() {
        val profiles = dao.getAll()
        if (profiles.any { it.id == DEFAULT_PROFILE_ID }) return

        val needsDefault = profiles.isEmpty() || dao.countDataForProfile(DEFAULT_PROFILE_ID) > 0
        if (!needsDefault) return

        val now = currentTimeMillis()
        val existing = dao.getById(DEFAULT_PROFILE_ID) // may be a soft-deleted row
        val restored = existing?.copy(
            isDeleted = false,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
        ) ?: ProfileEntity(
            id = DEFAULT_PROFILE_ID,
            name = "Me",
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(restored)
        syncScheduler.enqueue()
    }

    /** One-time copy of the old DataStore `app_theme` onto the default profile row. */
    private suspend fun migrateLegacyThemeIfNeeded() {
        if (prefs.isProfileThemeMigrated()) return
        val legacyTheme = prefs.appTheme.first()
        if (legacyTheme != null) {
            dao.getById(DEFAULT_PROFILE_ID)?.let {
                dao.upsert(it.copy(theme = legacyTheme, updatedAt = currentTimeMillis(), syncStatus = SyncStatus.PENDING))
            }
        }
        prefs.setProfileThemeMigrated()
    }
}
