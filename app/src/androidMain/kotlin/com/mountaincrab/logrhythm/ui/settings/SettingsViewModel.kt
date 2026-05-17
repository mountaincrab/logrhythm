package com.mountaincrab.logrhythm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.StoolTagEntity
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: UserPreferencesRepository,
    private val repository: EntryRepository,
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = prefs.appTheme
        .map { AppTheme.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DEEP_NAVY)

    val tags: StateFlow<List<StoolTagEntity>> = repository.observeAllStoolTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setAppTheme(theme.name) }
    }

    fun addTag(name: String) {
        viewModelScope.launch { repository.createStoolTag(name) }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch { repository.deleteStoolTag(id) }
    }
}
