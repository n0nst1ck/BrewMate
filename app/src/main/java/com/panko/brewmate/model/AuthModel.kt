package com.panko.brewmate.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.viewmodel.AuthInputState
import com.panko.brewmate.viewmodel.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthModel (private val authRepository: AuthRepository): ViewModel(){
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _inputState = MutableStateFlow(AuthInputState())
    val inputState: StateFlow<AuthInputState> = _inputState.asStateFlow()

    // --- State Update Functions ---

    fun onEmailChange(newEmail: String) {
        _inputState.update { it.copy(email = newEmail) }
        // Reset state on input change
        _uiState.update { AuthUiState.Idle }
    }

    fun onPasswordChange(newPassword: String) {
        _inputState.update { it.copy(password = newPassword) }
        // Reset state on input change
        _uiState.update { AuthUiState.Idle }
    }

    fun toggleAuthMode() {
        _inputState.update { it.copy(isSignIn = !it.isSignIn, password = "") }
        _uiState.update { AuthUiState.Idle }
    }

    // --- Authentication Logic ---

    fun authenticate() {
        // Simple client-side validation
        if (_inputState.value.email.isBlank() || _inputState.value.password.length < 6) {
            _uiState.update { AuthUiState.Error("Please enter a valid email and a password (min 6 chars).") }
            return
        }

        _uiState.update { AuthUiState.Loading } // Set loading state

        viewModelScope.launch {
            val result = if (_inputState.value.isSignIn) {
                authRepository.signIn(
                    _inputState.value.email,
                    _inputState.value.password
                )
            } else {
                authRepository.createUser(
                    _inputState.value.email,
                    _inputState.value.password
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update { AuthUiState.Success }
                },
                onFailure = { throwable ->
                    val message = throwable.localizedMessage ?: "Authentication failed."
                    _uiState.update { AuthUiState.Error(message) }
                }
            )
        }
    }
}