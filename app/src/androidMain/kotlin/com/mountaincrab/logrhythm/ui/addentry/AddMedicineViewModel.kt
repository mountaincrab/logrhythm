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
    val amount: String = "",
    val unit: String = "",
    val notes: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Logs a one-off dose. Regular doses come from the schedule and record themselves, so this
 * is for the exceptions: a painkiller, a rescue dose, something taken off-plan.
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
                            amount = e.amount,
                            unit = e.unit,
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
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onUnitChange(value: String) = _state.update { it.copy(unit = value) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    /** Selecting a medication pre-fills its usual dose, which the user can still override. */
    fun selectMedication(medication: MedicationEntity) = _state.update {
        it.copy(
            medicationId = medication.id,
            amount = medication.defaultAmount,
            unit = medication.defaultUnit,
        )
    }

    fun createMedicationAndSelect(name: String, form: MedicationForm, amount: String, unit: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = repository.saveMedication(
                name = name,
                form = form,
                defaultAmount = amount,
                defaultUnit = unit,
            )
            selectMedication(created)
        }
    }

    fun save() {
        val s = _state.value
        val medicationId = s.medicationId ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveManualDose(
                id = existingId,
                medicationId = medicationId,
                occurredAt = s.occurredAt,
                amount = s.amount,
                unit = s.unit,
                notes = s.notes,
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
