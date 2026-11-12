package com.example.eventia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val success: Boolean = false
)

/**
 * RF-11: registro e inicio de sesión con validación de correo
 * y contraseña de mínimo 8 caracteres; los errores 400/422 del
 * servidor se muestran tal cual llegan.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onModeChange(mode: AuthMode) {
        _uiState.value = AuthUiState(mode = mode)
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null, generalError = null)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null, generalError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, generalError = null)
    }

    fun submit() {
        val state = _uiState.value
        val emailError = if (!isValidEmail(state.email)) "Ingresa un correo válido" else null
        val passwordError = if (state.password.length < 8) "La contraseña debe tener al menos 8 caracteres" else null
        val nameError = if (state.mode == AuthMode.REGISTER && state.name.isBlank()) "Ingresa tu nombre" else null

        if (emailError != null || passwordError != null || nameError != null) {
            _uiState.value = state.copy(
                emailError = emailError,
                passwordError = passwordError,
                nameError = nameError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, generalError = null)
            val result = if (state.mode == AuthMode.LOGIN) {
                authRepository.login(state.email.trim(), state.password)
            } else {
                authRepository.register(state.name.trim(), state.email.trim(), state.password)
            }
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, success = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = e.message ?: "Ocurrió un error. Inténtalo de nuevo."
                    )
                }
            )
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }

    private fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email.trim())
}
