package com.laodisea.app.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.repository.AuthRepository
import com.laodisea.app.data.repository.SignUpResult
import com.laodisea.app.data.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !loading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        if (!isValidEmail(current.email)) {
            _state.update { it.copy(error = "Escribe un correo válido.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { auth.signIn(current.email, current.password) }
                .onSuccess { _state.update { it.copy(loading = false, signedIn = true) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }
}

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
    /** Mensaje cuando la cuenta se creó pero falta confirmar el correo. */
    val confirmEmailSent: Boolean = false,
) {
    val canSubmit: Boolean
        get() = displayName.isNotBlank() && email.isNotBlank() && password.isNotEmpty() &&
            confirmPassword.isNotEmpty() && !loading
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value.take(24), error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        val error = when {
            s.displayName.trim().length < 3 -> "El nombre de jugador debe tener al menos 3 letras."
            !isValidEmail(s.email) -> "Escribe un correo válido."
            s.password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
            s.password != s.confirmPassword -> "Las contraseñas no coinciden."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { auth.signUp(s.displayName, s.email, s.password) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            loading = false,
                            signedIn = result == SignUpResult.SignedIn,
                            confirmEmailSent = result == SignUpResult.ConfirmEmail,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }
}

private fun isValidEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
