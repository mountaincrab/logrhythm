package com.mountaincrab.logrhythm.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddFoodUiState(
    val occurredAt: Long = currentTimeMillis(),
    val items: String = "",
    val mealTag: MealTag? = null,
    val recent: List<String> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddFoodViewModel(
    private val repository: EntryRepository,
    private val existingId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(AddFoodUiState())
    val state: StateFlow<AddFoodUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val recent = repository.recentFoodItems(30).flatMap {
                // Split combined entries on commas so chip suggestions stay tidy.
                it.split(",").map { p -> p.trim() }.filter { p -> p.isNotEmpty() }
            }.distinct().take(12)
            _state.update { it.copy(recent = recent) }

            if (existingId != null) {
                repository.getFood(existingId)?.let { e ->
                    _state.update {
                        it.copy(occurredAt = e.occurredAt, items = e.items, mealTag = e.mealTag)
                    }
                }
            }
        }
    }

    fun onOccurredAtChange(value: Long) = _state.update { it.copy(occurredAt = value) }
    fun onItemsChange(value: String) = _state.update { it.copy(items = value) }
    fun onMealTagToggle(tag: MealTag) = _state.update {
        it.copy(mealTag = if (it.mealTag == tag) null else tag)
    }

    fun appendChip(chip: String) = _state.update {
        val current = it.items.trim()
        val next = if (current.isEmpty()) chip else "$current, $chip"
        it.copy(items = next)
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.items.isBlank()) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveFood(
                id = existingId,
                occurredAt = s.occurredAt,
                items = s.items.trim(),
                mealTag = s.mealTag,
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
