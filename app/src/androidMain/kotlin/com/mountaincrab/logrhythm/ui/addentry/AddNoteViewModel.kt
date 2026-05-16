package com.mountaincrab.logrhythm.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.data.repository.EntryRepository
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddNoteUiState(
    val occurredAt: Long = currentTimeMillis(),
    val content: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddNoteViewModel(
    private val repository: EntryRepository,
    private val existingId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(AddNoteUiState())
    val state: StateFlow<AddNoteUiState> = _state.asStateFlow()

    init {
        if (existingId != null) {
            viewModelScope.launch {
                repository.getNote(existingId)?.let { e ->
                    _state.update { it.copy(occurredAt = e.occurredAt, content = e.content) }
                }
            }
        }
    }

    fun onOccurredAtChange(value: Long) = _state.update { it.copy(occurredAt = value) }
    fun onContentChange(value: String) = _state.update { it.copy(content = value) }

    fun save() {
        val s = _state.value
        if (s.saving || s.content.isBlank()) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveNote(id = existingId, occurredAt = s.occurredAt, content = s.content.trim())
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
