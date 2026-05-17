package com.mountaincrab.logrhythm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
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

    val poopTags: StateFlow<List<PoopTagEntity>> = repository.observeAllPoopTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteTags: StateFlow<List<NoteTagEntity>> = repository.observeAllNoteTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setAppTheme(theme.name) }
    }

    fun addPoopTag(name: String) {
        viewModelScope.launch { repository.createPoopTag(name) }
    }

    fun deletePoopTag(id: String) {
        viewModelScope.launch { repository.deletePoopTag(id) }
    }

    fun addNoteTag(name: String) {
        viewModelScope.launch { repository.createNoteTag(name) }
    }

    fun deleteNoteTag(id: String) {
        viewModelScope.launch { repository.deleteNoteTag(id) }
    }
}
