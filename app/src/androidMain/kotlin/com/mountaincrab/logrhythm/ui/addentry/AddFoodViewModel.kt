package com.mountaincrab.logrhythm.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.FoodItemWithComponents
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.data.repository.FoodEntryLineInput
import com.mountaincrab.logrhythm.data.repository.FoodRepository
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodLineDraft(
    val id: String,
    val foodItemId: String? = null,
    val quantity: String = "",
    val customText: String? = null,
    val componentAmounts: Map<String, String> = emptyMap(),
)

data class AddFoodUiState(
    val occurredAt: Long = currentTimeMillis(),
    val lines: List<FoodLineDraft> = emptyList(),
    val mealTag: MealTag? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddFoodViewModel(
    private val repository: FoodRepository,
    private val existingId: String?,
) : ViewModel() {
    private val _state = MutableStateFlow(AddFoodUiState())
    val state: StateFlow<AddFoodUiState> = _state.asStateFlow()

    val foodItems: StateFlow<List<FoodItemWithComponents>> = repository.observeFoodItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val foodItemsForLookup: StateFlow<List<FoodItemWithComponents>> = repository.observeFoodItemsForLookup()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val components: StateFlow<List<TrackedComponentEntity>> = repository.observeComponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val componentsForLookup: StateFlow<List<TrackedComponentEntity>> = repository.observeComponentsForLookup()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (existingId != null) {
            viewModelScope.launch {
                repository.getEntry(existingId)?.let { food ->
                    _state.update {
                        it.copy(
                            occurredAt = food.entry.occurredAt,
                            mealTag = food.entry.mealTag,
                            lines = food.lines.map { resolved ->
                                FoodLineDraft(
                                    id = resolved.line.id,
                                    foodItemId = resolved.line.foodItemId,
                                    quantity = resolved.line.quantity?.let(::displayNumber).orEmpty(),
                                    customText = resolved.line.customText,
                                    componentAmounts = if (resolved.line.foodItemId == null) {
                                        resolved.componentAmounts.mapValues { (_, value) -> displayNumber(value) }
                                    } else emptyMap(),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    fun onOccurredAtChange(value: Long) = _state.update { it.copy(occurredAt = value) }
    fun onMealTagToggle(tag: MealTag) = _state.update {
        it.copy(mealTag = if (it.mealTag == tag) null else tag)
    }

    fun addSavedItem(itemId: String) = _state.update { state ->
        state.copy(lines = state.lines + FoodLineDraft(id = com.mountaincrab.logrhythm.util.randomUUID(), foodItemId = itemId, quantity = "1"))
    }

    fun addCustomItem(text: String, amounts: Map<String, String>) = _state.update { state ->
        state.copy(
            lines = state.lines + FoodLineDraft(
                id = com.mountaincrab.logrhythm.util.randomUUID(),
                customText = text.trim(),
                componentAmounts = amounts.filterValues { it.toDoubleOrNull()?.let { n -> n > 0.0 } == true },
            ),
        )
    }

    fun updateQuantity(lineId: String, value: String) = _state.update { state ->
        state.copy(lines = state.lines.map { if (it.id == lineId) it.copy(quantity = value) else it })
    }

    fun adjustQuantity(lineId: String, delta: Int) = _state.update { state ->
        state.copy(
            lines = state.lines.map { line ->
                if (line.id != lineId) line else {
                    val current = line.quantity.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 1.0
                    line.copy(quantity = displayNumber((current + delta).coerceAtLeast(1.0)))
                }
            },
        )
    }

    fun removeLine(lineId: String) = _state.update { state ->
        state.copy(lines = state.lines.filterNot { it.id == lineId })
    }

    fun reorderLine(lineId: String, targetIndex: Int) = _state.update { state ->
        val index = state.lines.indexOfFirst { it.id == lineId }
        if (index < 0 || targetIndex !in state.lines.indices || index == targetIndex) state else {
            val reordered = state.lines.toMutableList()
            val item = reordered.removeAt(index)
            reordered.add(targetIndex, item)
            state.copy(lines = reordered)
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving || current.lines.isEmpty()) return
        val inputs = current.lines.mapNotNull { line ->
            if (line.foodItemId != null) {
                val quantity = line.quantity.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 1.0 } ?: return
                FoodEntryLineInput(id = line.id, foodItemId = line.foodItemId, quantity = quantity)
            } else {
                val text = line.customText?.trim()?.takeIf { it.isNotEmpty() } ?: return
                FoodEntryLineInput(
                    id = line.id,
                    customText = text,
                    componentAmounts = line.componentAmounts.mapNotNull { (componentId, value) ->
                        value.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }?.let { componentId to it }
                    }.toMap(),
                )
            }
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveEntry(existingId, current.occurredAt, current.mealTag, inputs)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    private fun displayNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
