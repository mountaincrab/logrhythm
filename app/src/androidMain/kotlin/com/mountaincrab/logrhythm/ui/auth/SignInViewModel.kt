package com.mountaincrab.logrhythm.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.logrhythm.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignInViewModel(private val authRepo: AuthRepository) : ViewModel() {

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Error(val message: String) : State()
    }

    val state: StateFlow<State> = MutableStateFlow(State.Idle)
    private val _state get() = state as MutableStateFlow

    fun signInWithGoogle(idToken: String) = viewModelScope.launch {
        _state.value = State.Loading
        try {
            authRepo.signInWithGoogle(idToken)
            _state.value = State.Idle
        } catch (e: Exception) {
            _state.value = State.Error(e.message ?: "Google sign-in failed")
        }
    }

    fun signInAnonymously() = viewModelScope.launch {
        _state.value = State.Loading
        try {
            authRepo.signInAnonymously()
            _state.value = State.Idle
        } catch (e: Exception) {
            _state.value = State.Error(e.message ?: "Anonymous sign-in failed")
        }
    }
}
