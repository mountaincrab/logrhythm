package com.mountaincrab.logrhythm.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.StoolTagEntity
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryDetailUiState(
    val poop: PoopEntryEntity? = null,
    val poopTags: List<StoolTagEntity> = emptyList(),
    val food: FoodEntryEntity? = null,
    val note: NoteEntryEntity? = null,
    val foodWindow: List<FoodEntryEntity> = emptyList(),
    val deleted: Boolean = false,
)

class EntryDetailViewModel(
    private val repository: EntryRepository,
    val kind: String,
    private val entryId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(EntryDetailUiState())
    val state: StateFlow<EntryDetailUiState> = _state.asStateFlow()

    init { load() }

    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            when (kind) {
                "poop" -> {
                    val p = repository.getPoop(entryId) ?: return@launch
                    val end = p.occurredAt
                    val start = end - 24L * 60 * 60 * 1000
                    val foods = repository.foodsInRange(start, end)
                    val tags = repository.getPoopTags(entryId)
                    _state.update { it.copy(poop = p, poopTags = tags, foodWindow = foods) }
                }
                "food" -> _state.update { it.copy(food = repository.getFood(entryId)) }
                "note" -> _state.update { it.copy(note = repository.getNote(entryId)) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            when (kind) {
                "poop" -> repository.deletePoop(entryId)
                "food" -> repository.deleteFood(entryId)
                "note" -> repository.deleteNote(entryId)
            }
            _state.update { it.copy(deleted = true) }
        }
    }
}
