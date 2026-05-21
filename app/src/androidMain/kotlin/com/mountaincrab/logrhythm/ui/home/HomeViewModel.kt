package com.mountaincrab.logrhythm.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
import com.mountaincrab.logrhythm.sync.SyncScheduler
import com.mountaincrab.logrhythm.ui.util.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val days: List<DayGroup> = emptyList(),
    val todayPoopCount: Int = 0,
    val todayWorstRating: Int? = null,
)

data class DayGroup(
    val date: LocalDate,
    val entries: List<TimelineEntry>,
)

class HomeViewModel(
    repository: EntryRepository,
    private val syncScheduler: SyncScheduler,
    workManager: WorkManager,
) : ViewModel() {

    init {
        syncScheduler.enqueue()
    }

    val isSyncing: StateFlow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(SyncScheduler.WORK_NAME)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<HomeUiState> = repository.observeTimeline()
        .map { all ->
            val today = LocalDate.now()
            val grouped = all.groupBy { it.occurredAt.toLocalDate() }
                .toSortedMap(compareByDescending { it })
                .map { (date, entries) -> DayGroup(date, entries) }
            val todayPoops = all.filterIsInstance<TimelineEntry.Poop>()
                .filter { it.entity.occurredAt.toLocalDate() == today }
            HomeUiState(
                days = grouped,
                todayPoopCount = todayPoops.size,
                todayWorstRating = todayPoops.maxOfOrNull { it.entity.blood },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun sync() = syncScheduler.enqueue()
}
