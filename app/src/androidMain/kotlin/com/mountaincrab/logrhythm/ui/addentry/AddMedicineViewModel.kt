package com.mountaincrab.logrhythm.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.repository.MedicationRepository
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddMedicineUiState(
    val occurredAt: Long = currentTimeMillis(),
    val medicationId: String? = null,
    val quantity: String = "1",
    val notes: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Logs a one-off dose, and edits any dose — including one a schedule added, which is how
 * you correct the quantity you actually took.
 */
class AddMedicineViewModel(
    private val repository: MedicationRepository,
    private val existingId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(AddMedicineUiState())
    val state: StateFlow<AddMedicineUiState> = _state.asStateFlow()

    val medications: StateFlow<List<MedicationEntity>> = repository.observeMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (existingId != null) {
                repository.getEntry(existingId)?.let { e ->
                    _state.update {
                        it.copy(
                            occurredAt = e.occurredAt,
                            medicationId = e.medicationId,
                            quantity = e.quantity,
                            notes = e.notes.orEmpty(),
                        )
                    }
                }
            } else {
                // Single-medication users shouldn't have to pick every time.
                repository.getMedications().singleOrNull()?.let { selectMedication(it) }
            }
        }
    }

    fun onOccurredAtChange(value: Long) = _state.update { it.copy(occurredAt = value) }
    fun onQuantityChange(value: String) = _state.update { it.copy(quantity = value) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun selectMedication(medication: MedicationEntity) = _state.update {
        it.copy(medicationId = medication.id)
    }

    fun createMedicationAndSelect(name: String, form: MedicationForm, dose: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = repository.saveMedication(name = name, form = form, dose = dose)
            selectMedication(created)
        }
    }

    fun save() {
        val s = _state.value
        val medicationId = s.medicationId ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveDose(
                id = existingId,
                medicationId = medicationId,
                occurredAt = s.occurredAt,
                quantity = s.quantity,
                notes = s.notes,
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
