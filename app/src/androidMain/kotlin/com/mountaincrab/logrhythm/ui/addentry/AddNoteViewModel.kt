package com.mountaincrab.logrhythm.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddNoteUiState(
    val occurredAt: Long = currentTimeMillis(),
    val content: String = "",
    val caffeine: Boolean = false,
    val alcohol: Boolean = false,
    val selectedNoteTagIds: Set<String> = emptySet(),
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddNoteViewModel(
    private val repository: EntryRepository,
    private val existingId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(AddNoteUiState())
    val state: StateFlow<AddNoteUiState> = _state.asStateFlow()

    val allNoteTags: StateFlow<List<NoteTagEntity>> = repository.observeAllNoteTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (existingId != null) {
            viewModelScope.launch {
                repository.getNote(existingId)?.let { e ->
                    val existingTags = repository.getNoteTags(existingId)
                    _state.update {
                        it.copy(
                            occurredAt = e.occurredAt,
                            content = e.content,
                            caffeine = e.caffeine,
                            alcohol = e.alcohol,
                            selectedNoteTagIds = existingTags.map { t -> t.id }.toSet(),
                        )
                    }
                }
            }
        }
    }

    fun onOccurredAtChange(value: Long) = _state.update { it.copy(occurredAt = value) }
    fun onContentChange(value: String) = _state.update { it.copy(content = value) }
    fun onCaffeineToggle() = _state.update { it.copy(caffeine = !it.caffeine) }
    fun onAlcoholToggle() = _state.update { it.copy(alcohol = !it.alcohol) }

    fun onNoteTagToggle(tagId: String) = _state.update {
        val new = if (tagId in it.selectedNoteTagIds) it.selectedNoteTagIds - tagId else it.selectedNoteTagIds + tagId
        it.copy(selectedNoteTagIds = new)
    }

    fun createNoteTagAndSelect(name: String) {
        viewModelScope.launch {
            val tag = repository.createNoteTag(name)
            _state.update { it.copy(selectedNoteTagIds = it.selectedNoteTagIds + tag.id) }
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.content.isBlank() && !s.caffeine && !s.alcohol && s.selectedNoteTagIds.isEmpty()) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveNote(
                id = existingId,
                occurredAt = s.occurredAt,
                content = s.content.trim(),
                caffeine = s.caffeine,
                alcohol = s.alcohol,
                noteTagIds = s.selectedNoteTagIds,
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
