package com.mountaincrab.logrhythm.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfileId: String = "",
)

class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository,
) : ViewModel() {

    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProfileId: StateFlow<String> = profileRepository.activeProfileId

    fun selectProfile(id: String) {
        viewModelScope.launch { profileRepository.setActiveProfile(id) }
    }

    fun addProfile(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = profileRepository.createProfile(name)
            profileRepository.setActiveProfile(created.id)
        }
    }

    fun renameProfile(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { profileRepository.renameProfile(id, name) }
    }

    /** Cascade-deletes the profile and its data. No-op if it's the last profile. */
    fun deleteProfile(id: String) {
        viewModelScope.launch {
            val remaining = profiles.value.filter { it.id != id }
            if (remaining.isEmpty()) return@launch
            if (activeProfileId.value == id) {
                profileRepository.setActiveProfile(remaining.first().id)
            }
            entryRepository.deleteProfileData(id)
            profileRepository.deleteProfile(id)
        }
    }
}
