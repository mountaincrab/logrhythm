package com.mountaincrab.logrhythm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.model.StoolSystem
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = prefs.appTheme
        .map { AppTheme.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DEEP_NAVY)

    val stoolSystem: StateFlow<StoolSystem> = prefs.stoolSystem
        .map { StoolSystem.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoolSystem.BRISTOL)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setAppTheme(theme.name) }
    }

    fun setStoolSystem(value: StoolSystem) {
        viewModelScope.launch { prefs.setStoolSystem(value.name) }
    }
}
