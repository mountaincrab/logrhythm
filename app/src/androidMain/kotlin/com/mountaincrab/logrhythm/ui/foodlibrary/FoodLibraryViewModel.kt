package com.mountaincrab.logrhythm.ui.foodlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.FoodItemWithComponents
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.data.repository.FoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodLibraryViewModel(private val repository: FoodRepository) : ViewModel() {
    private val _lockedComponentIds = MutableStateFlow<Set<String>>(emptySet())
    val lockedComponentIds: StateFlow<Set<String>> = _lockedComponentIds
    val components: StateFlow<List<TrackedComponentEntity>> = repository.observeComponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val archivedComponents: StateFlow<List<TrackedComponentEntity>> = repository.observeArchivedComponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items: StateFlow<List<FoodItemWithComponents>> = repository.observeFoodItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val archivedItems: StateFlow<List<FoodItemWithComponents>> = repository.observeArchivedFoodItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.observeComponentsForLookup().collectLatest { values ->
                _lockedComponentIds.value = values.mapNotNull { component ->
                    component.id.takeIf { repository.isComponentUnitLocked(it) }
                }.toSet()
            }
        }
    }

    fun saveComponent(id: String?, name: String, unit: String) {
        viewModelScope.launch { repository.saveComponent(id, name, unit) }
    }

    fun archiveComponent(id: String) {
        viewModelScope.launch { repository.setComponentArchived(id, true) }
    }

    fun restoreComponent(id: String) {
        viewModelScope.launch { repository.setComponentArchived(id, false) }
    }

    fun saveItem(id: String?, name: String, icon: String, amount: String, unit: String, componentAmounts: Map<String, Double>) {
        viewModelScope.launch { repository.saveFoodItem(id, name, icon, amount, unit, componentAmounts) }
    }

    fun archiveItem(id: String) {
        viewModelScope.launch { repository.setFoodItemArchived(id, true) }
    }

    fun restoreItem(id: String) {
        viewModelScope.launch { repository.setFoodItemArchived(id, false) }
    }
}
