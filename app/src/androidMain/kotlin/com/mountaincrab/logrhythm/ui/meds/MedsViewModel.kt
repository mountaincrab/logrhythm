package com.mountaincrab.logrhythm.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.local.entity.dose
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

/** A scheduled dose paired with its catalog medication (null only if that vanished entirely). */
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

    /** Archived medications and schedules, for the Archived sections that restore them. */
    val archivedMedications: StateFlow<List<MedicationEntity>> = repository.observeArchivedMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val archivedScheduleList: StateFlow<List<MedicationScheduleEntity>> =
        repository.observeArchivedSchedules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Schedule rows resolve their medication through the archived-inclusive catalog: a
     * schedule can outlive its medication leaving the pickers, and it still has to read.
     */
    private val catalogForLookup: StateFlow<List<MedicationEntity>> =
        combine(medications, archivedMedications) { live, archived -> live + archived }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val scheduleRows: StateFlow<List<ScheduleRow>> =
        combine(schedules, catalogForLookup) { scheduleList, medicationList ->
            val byId = medicationList.associateBy { it.id }
            scheduleList.map { ScheduleRow(it, byId[it.medicationId]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedScheduleRows: StateFlow<List<ScheduleRow>> =
        combine(archivedScheduleList, catalogForLookup) { scheduleList, medicationList ->
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

    fun saveMedication(
        id: String?,
        name: String,
        form: MedicationForm,
        doseAmount: String,
        doseUnit: String,
    ) {
        viewModelScope.launch { repository.saveMedication(id, name, form, doseAmount, doseUnit) }
    }

    fun archiveMedication(id: String) {
        viewModelScope.launch { repository.archiveMedication(id) }
    }

    fun restoreMedication(id: String) {
        viewModelScope.launch { repository.restoreMedication(id) }
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

    fun archiveSchedule(id: String) {
        viewModelScope.launch { repository.archiveSchedule(id) }
    }

    /** Restoring re-anchors the schedule to today — see [MedicationRepository.restoreSchedule]. */
    fun restoreSchedule(id: String) {
        viewModelScope.launch { repository.restoreSchedule(id) }
    }

    fun setScheduleActive(id: String, active: Boolean) {
        viewModelScope.launch { repository.setScheduleActive(id, active) }
    }
}
