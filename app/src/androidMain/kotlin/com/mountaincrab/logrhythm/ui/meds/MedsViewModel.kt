package com.mountaincrab.logrhythm.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.model.DoseStatus
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.repository.MedicationRepository
import com.mountaincrab.logrhythm.data.repository.UpcomingDose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A scheduled dose paired with its catalog medication (null if that was deleted). */
data class ScheduleRow(
    val schedule: MedicationScheduleEntity,
    val medication: MedicationEntity?,
) {
    val name: String get() = medication?.name ?: "Unknown medication"
}

/** Today's doses, mixing what's already recorded with what's still to come. */
sealed class TodayDose(open val at: Long) {
    data class Recorded(val entry: MedicationEntryEntity) : TodayDose(entry.occurredAt)
    data class Upcoming(val dose: UpcomingDose) : TodayDose(dose.dueAt)
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

    private val todayEntries: StateFlow<List<MedicationEntryEntity>> =
        repository.observeEntriesForDay(LocalDate.now())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val upcoming = MutableStateFlow<List<UpcomingDose>>(emptyList())

    val today: StateFlow<List<TodayDose>> =
        combine(todayEntries, upcoming) { recorded, due ->
            (recorded.map { TodayDose.Recorded(it) } + due.map { TodayDose.Upcoming(it) })
                .sortedBy { it.at }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Fill in anything that came due while the app was closed, then work out what's left today.
        viewModelScope.launch {
            repository.materialiseDueDoses()
            refreshUpcoming()
        }
        // Editing the schedule immediately changes what's still owed today.
        viewModelScope.launch { schedules.collect { refreshUpcoming() } }
    }

    private suspend fun refreshUpcoming() {
        upcoming.value = repository.upcomingToday()
    }

    fun saveMedication(id: String?, name: String, form: MedicationForm, amount: String, unit: String) {
        viewModelScope.launch { repository.saveMedication(id, name, form, amount, unit) }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
            refreshUpcoming()
        }
    }

    fun saveSchedule(
        id: String?,
        medicationId: String,
        amount: String,
        unit: String,
        timeMinutes: Int,
        repeatRule: RepeatRule,
        daysMask: Int,
    ) {
        viewModelScope.launch {
            repository.saveSchedule(id, medicationId, amount, unit, timeMinutes, repeatRule, daysMask)
            repository.materialiseDueDoses()
            refreshUpcoming()
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
            refreshUpcoming()
        }
    }

    fun setScheduleActive(id: String, active: Boolean) {
        viewModelScope.launch {
            repository.setScheduleActive(id, active)
            refreshUpcoming()
        }
    }

    fun setDoseStatus(entryId: String, status: DoseStatus, amount: String? = null, unit: String? = null) {
        viewModelScope.launch { repository.setDoseStatus(entryId, status, amount, unit) }
    }

    /** Acts on a dose that hasn't come due yet — records it now instead of waiting. */
    fun confirmUpcoming(dose: UpcomingDose, status: DoseStatus) {
        viewModelScope.launch {
            repository.confirmUpcoming(dose, status)
            refreshUpcoming()
        }
    }
}
