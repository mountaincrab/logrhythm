package com.mountaincrab.logrhythm.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = profileRepository.activeProfile
        .map { AppTheme.fromName(it?.theme) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DEEP_NAVY)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { profileRepository.setActiveProfileTheme(theme.name) }
    }
}
