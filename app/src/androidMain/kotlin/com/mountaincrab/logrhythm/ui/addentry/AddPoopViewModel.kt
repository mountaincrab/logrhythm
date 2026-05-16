package com.mountaincrab.logrhythm.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.model.StoolSystem
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddPoopUiState(
    val occurredAt: Long = currentTimeMillis(),
    val bristolTypes: Set<Int> = setOf(4),
    val blood: Int = 1,
    val notes: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddPoopViewModel(
    private val repository: EntryRepository,
    private val prefs: UserPreferencesRepository,
    private val existingId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(AddPoopUiState())
    val state: StateFlow<AddPoopUiState> = _state.asStateFlow()

    val stoolSystem: StateFlow<StoolSystem> = prefs.stoolSystem
        .map { StoolSystem.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoolSystem.BRISTOL)

    init {
        if (existingId != null) {
            viewModelScope.launch {
                repository.getPoop(existingId)?.let { e ->
                    val types = e.bristolTypes.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .toSet()
                        .ifEmpty { setOf(4) }
                    _state.value = AddPoopUiState(
                        occurredAt = e.occurredAt,
                        bristolTypes = types,
                        blood = e.blood,
                        notes = e.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onOccurredAtChange(value: Long) = _state.update { it.copy(occurredAt = value) }

    fun onBristolToggle(value: Int) = _state.update {
        val new = if (value in it.bristolTypes) it.bristolTypes - value else it.bristolTypes + value
        it.copy(bristolTypes = new.ifEmpty { setOf(value) })
    }

    fun onBloodChange(value: Int) = _state.update { it.copy(blood = value) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun onStoolSystemChange(system: StoolSystem) {
        viewModelScope.launch { prefs.setStoolSystem(system.name) }
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.savePoop(
                id = existingId,
                occurredAt = s.occurredAt,
                bristolTypes = s.bristolTypes,
                blood = s.blood,
                notes = s.notes,
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
