package com.mountaincrab.logrhythm.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.data.repository.MedicationRepository
import com.mountaincrab.logrhythm.data.repository.ProfileRepository
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
import com.mountaincrab.logrhythm.sync.SyncScheduler
import com.mountaincrab.logrhythm.ui.util.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HomeEntryType {
    POOP,
    FOOD,
    NOTE,
    MEDICINE,
}

data class HomeUiState(
    val days: List<DayGroup> = emptyList(),
    val todayPoopCount: Int = 0,
    val todayWorstRating: Int? = null,
    val enabledEntryTypes: Set<HomeEntryType> = HomeEntryType.entries.toSet(),
    val totalEntryCount: Int = 0,
    val loading: Boolean = true,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
) {
    val filtersActive: Boolean
        get() = enabledEntryTypes.size != HomeEntryType.entries.size
}

data class DayGroup(
    val date: LocalDate,
    val entries: List<TimelineEntry>,
    val totalEntryCount: Int,
)

class HomeViewModel(
    private val repository: EntryRepository,
    private val profileRepository: ProfileRepository,
    private val medicationRepository: MedicationRepository,
    private val syncScheduler: SyncScheduler,
    workManager: WorkManager,
) : ViewModel() {

    // Lower bound of the loaded timeline window (entries with occurredAt >= watermark are
    // shown). Starts at MAX_VALUE (empty) until the first page is computed; loadMore() lowers
    // it one page at a time. MIN_VALUE means the whole history is loaded.
    private val watermark = MutableStateFlow(Long.MAX_VALUE)
    private val initialised = MutableStateFlow(false)
    private val hasMore = MutableStateFlow(false)
    private val loadingMore = MutableStateFlow(false)
    private val enabledEntryTypes = MutableStateFlow(HomeEntryType.entries.toSet())

    init {
        syncScheduler.enqueue()
        // Reset to the first page whenever the active profile changes. Scheduled doses that
        // came due while the app was closed are written first, so the first page already
        // includes them rather than having them pop in once the sync worker gets around to it.
        viewModelScope.launch {
            profileRepository.activeProfileId.collectLatest {
                initialised.value = false
                medicationRepository.materialiseDueDoses()
                watermark.value = Long.MAX_VALUE
                val wm = repository.timelineWatermark(before = Long.MAX_VALUE, pageSize = PAGE_SIZE)
                watermark.value = wm
                hasMore.value = wm != Long.MIN_VALUE
                initialised.value = true
            }
        }
    }

    /** Extend the loaded window one page older. Safe to call repeatedly / while scrolling. */
    fun loadMore() {
        if (loadingMore.value || !hasMore.value || !initialised.value) return
        viewModelScope.launch {
            loadingMore.value = true
            val next = repository.timelineWatermark(before = watermark.value, pageSize = PAGE_SIZE)
            watermark.value = next
            hasMore.value = next != Long.MIN_VALUE
            loadingMore.value = false
        }
    }

    fun toggleEntryType(type: HomeEntryType) {
        enabledEntryTypes.update { current ->
            if (type in current) current - type else current + type
        }
    }

    /** Clear the active filter, restoring every entry type. */
    fun clearEntryFilters() {
        enabledEntryTypes.value = HomeEntryType.entries.toSet()
    }

    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProfile: StateFlow<ProfileEntity?> = profileRepository.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectProfile(id: String) {
        viewModelScope.launch { profileRepository.setActiveProfile(id) }
    }

    fun addProfile(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = profileRepository.createProfile(name)
            profileRepository.setActiveProfile(created.id)
        }
    }

    val isSyncing: StateFlow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(SyncScheduler.WORK_NAME)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTimeline(watermark),
        initialised,
        hasMore,
        loadingMore,
        enabledEntryTypes,
    ) { all, isInit, more, loadingM, enabledTypes ->
        val today = LocalDate.now()
        val grouped = all.groupBy { it.occurredAt.toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .mapNotNull { (date, entries) ->
                val visibleEntries = entries.filter { it.homeEntryType() in enabledTypes }
                visibleEntries.takeIf { it.isNotEmpty() }?.let {
                    DayGroup(
                        date = date,
                        entries = it,
                        totalEntryCount = entries.size,
                    )
                }
            }
        val todayPoops = all.filterIsInstance<TimelineEntry.Poop>()
            .filter { it.entity.occurredAt.toLocalDate() == today }
        HomeUiState(
            days = grouped,
            todayPoopCount = todayPoops.size,
            todayWorstRating = todayPoops.maxOfOrNull { it.entity.blood },
            enabledEntryTypes = enabledTypes,
            totalEntryCount = all.size,
            loading = !isInit,
            hasMore = more,
            loadingMore = loadingM,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun sync() = syncScheduler.enqueue()

    private companion object {
        const val PAGE_SIZE = 50
    }
}

private fun TimelineEntry.homeEntryType(): HomeEntryType = when (this) {
    is TimelineEntry.Poop -> HomeEntryType.POOP
    is TimelineEntry.Food -> HomeEntryType.FOOD
    is TimelineEntry.Note -> HomeEntryType.NOTE
    is TimelineEntry.Medication -> HomeEntryType.MEDICINE
}
