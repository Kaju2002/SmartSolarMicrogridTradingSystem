/*
 * File: RegisterViewModel.kt
 * Description: Register form state and submit logic
 */
package com.solargrid.prosumer.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solargrid.prosumer.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val nic: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val registered: Boolean = false,
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, error = null) }
    }

    fun onNicChange(value: String) {
        _uiState.update { it.copy(nic = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun togglePasswordVisible() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun register() {
        val state = _uiState.value
        when {
            state.fullName.isBlank() -> {
                _uiState.update { it.copy(error = "Enter your full name.") }
                return
            }
            state.nic.isBlank() -> {
                _uiState.update { it.copy(error = "Enter your NIC.") }
                return
            }
            state.phoneNumber.isBlank() -> {
                _uiState.update { it.copy(error = "Enter your phone number.") }
                return
            }
            state.email.isBlank() -> {
                _uiState.update { it.copy(error = "Enter your email.") }
                return
            }
            state.password.length < 6 -> {
                _uiState.update { it.copy(error = "Password must be at least 6 characters.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = authRepository.register(
                fullName = state.fullName,
                nic = state.nic,
                email = state.email,
                phoneNumber = state.phoneNumber,
                password = state.password,
            )
            result.fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            registered = true,
                            successMessage = body.message.ifBlank {
                                "Registered. Wait for operator approval, then sign in."
                            },
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = err.message ?: "Registration failed",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RegisterViewModel(authRepository) as T
                }
            }
    }
}
