package com.mountaincrab.logrhythm.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A scheduled dose paired with its catalog medication (null if that was deleted). */
data class ScheduleRow(
    val schedule: MedicationScheduleEntity,
    val medication: MedicationEntity?,
) {
    val name: String get() = medication?.name ?: "Unknown medication"
    val dose: String get() = medication?.dose.orEmpty()
}

class MedsViewModel(
    private val repository: MedicationRepository,
) : ViewModel() {

    val medications: StateFlow<List<MedicationEntity>> = repository.observeMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val schedules: StateFlow<List<MedicationScheduleEntity>> = repository.observeSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val scheduleRows: StateFlow<List<ScheduleRow>> =
        combine(schedules, medications) { scheduleList, medicationList ->
            val byId = medicationList.associateBy { it.id }
            scheduleList.map { ScheduleRow(it, byId[it.medicationId]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Flat list (one row per scheduled dose) vs. grouped by medication. The stored data is the
     * same either way — a scheduled dose carries its medicationId — so this is purely how the
     * schedule reads.
     */
    private val _groupByMedication = MutableStateFlow(false)
    val groupByMedication: StateFlow<Boolean> = _groupByMedication.asStateFlow()
    fun toggleGrouping() { _groupByMedication.value = !_groupByMedication.value }

    init {
        // Fill in anything that came due while the app was closed.
        viewModelScope.launch { repository.materialiseDueDoses() }
    }

    fun saveMedication(id: String?, name: String, form: MedicationForm, dose: String) {
        viewModelScope.launch { repository.saveMedication(id, name, form, dose) }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch { repository.deleteMedication(id) }
    }

    fun saveSchedule(
        id: String?,
        medicationId: String,
        quantity: String,
        timeMinutes: Int,
        repeatRule: RepeatRule,
        daysMask: Int,
    ) {
        viewModelScope.launch {
            repository.saveSchedule(id, medicationId, quantity, timeMinutes, repeatRule, daysMask)
            repository.materialiseDueDoses()
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch { repository.deleteSchedule(id) }
    }

    fun setScheduleActive(id: String, active: Boolean) {
        viewModelScope.launch { repository.setScheduleActive(id, active) }
    }
}
