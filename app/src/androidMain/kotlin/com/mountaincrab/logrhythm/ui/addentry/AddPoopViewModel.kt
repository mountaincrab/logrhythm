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
    val bristol: Int = 4,
    val rating: Int = 1,
    val notes: String = "",
    val medsMissed: Boolean = false,
    val caffeine: Boolean = false,
    val alcohol: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddPoopViewModel(
    private val repository: EntryRepository,
    prefs: UserPreferencesRepository,
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
                    _state.value = AddPoopUiState(
                        occurredAt = e.occurredAt,
                        bristol = e.bristol,
                        rating = e.rating,
                        notes = e.notes.orEmpty(),
                        medsMissed = e.medsMissed,
                        caffeine = e.caffeine,
                        alcohol = e.alcohol,
                    )
                }
            }
        }
    }

    fun onOccurredAtChange(value: Long) = _state.update { it.copy(occurredAt = value) }
    fun onBristolChange(value: Int) = _state.update { it.copy(bristol = value) }
    fun onRatingChange(value: Int) = _state.update { it.copy(rating = value) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }
    fun onMedsToggle() = _state.update { it.copy(medsMissed = !it.medsMissed) }
    fun onCaffeineToggle() = _state.update { it.copy(caffeine = !it.caffeine) }
    fun onAlcoholToggle() = _state.update { it.copy(alcohol = !it.alcohol) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.savePoop(
                id = existingId,
                occurredAt = s.occurredAt,
                bristol = s.bristol,
                rating = s.rating,
                notes = s.notes,
                medsMissed = s.medsMissed,
                caffeine = s.caffeine,
                alcohol = s.alcohol,
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
