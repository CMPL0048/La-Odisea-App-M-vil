package com.laodisea.app.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.isEmailRateLimit
import com.laodisea.app.data.isNetworkError
import com.laodisea.app.data.repository.AuthRepository
import com.laodisea.app.data.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mensaje tras pedir el código. Es el mismo exista o no la cuenta, para no revelar qué correos están registrados. */
const val RESET_CODE_SENT_MESSAGE = "Si el correo está registrado, te enviamos un código de 6 dígitos."

/** Segundos de espera antes de poder pedir otro código. */
private const val RESEND_SECONDS = 60

/**
 * Solo la falta de red o el límite de envíos se le muestran al jugador. Cualquier otro error se trata
 * como enviado, porque decir «ese correo no existe» revelaría qué cuentas hay.
 */
private fun Throwable.blocksCodeSending() = isNetworkError() || isEmailRateLimit()

data class ForgotPasswordUiState(
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    /** Se pidió el código: la pantalla pasa a escribirlo. Se consume con [ForgotPasswordViewModel.onCodeSentHandled]. */
    val codeSent: Boolean = false,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && !loading
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
) : ViewModel() {
    // El correo llega prellenado desde el inicio de sesión (argumento de la ruta).
    private val _state = MutableStateFlow(ForgotPasswordUiState(email = savedStateHandle.get<String>("email").orEmpty()))
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onCodeSentHandled() = _state.update { it.copy(codeSent = false) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        if (!isValidEmail(current.email)) {
            _state.update { it.copy(error = "Escribe un correo válido.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { auth.sendPasswordResetCode(current.email) }
                .onSuccess { _state.update { it.copy(loading = false, codeSent = true) } }
                .onFailure { e ->
                    _state.update {
                        if (e.blocksCodeSending()) it.copy(loading = false, error = e.toUserMessage())
                        else it.copy(loading = false, codeSent = true)
                    }
                }
        }
    }
}

data class ResetPasswordUiState(
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    /** Aviso de que se mandó (o reenvió) el código. */
    val info: String? = RESET_CODE_SENT_MESSAGE,
    /** Segundos que faltan para poder reenviar el código; 0 = ya se puede. */
    val resendSeconds: Int = RESEND_SECONDS,
    /** El código ya se validó (hay sesión); si falla el cambio de contraseña no se vuelve a validar. */
    val verified: Boolean = false,
    val signedIn: Boolean = false,
    /** Se abandonó la recuperación después de validar el código y ya se cerró esa sesión. */
    val signedOut: Boolean = false,
) {
    val canSubmit: Boolean
        get() = code.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && !loading
    val canResend: Boolean get() = resendSeconds == 0 && !loading && !verified
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ResetPasswordUiState(email = savedStateHandle.get<String>("email").orEmpty()))
    val state: StateFlow<ResetPasswordUiState> = _state.asStateFlow()

    private var countdown: Job? = null

    init {
        // Se llega aquí justo después de pedir el código.
        startCountdown()
    }

    fun onCodeChange(value: String) = _state.update { it.copy(code = sanitizeResetCode(value), error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun resend() {
        val current = _state.value
        if (!current.canResend) return
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            val error = runCatching { auth.sendPasswordResetCode(current.email) }
                .exceptionOrNull()
                ?.takeIf { it.blocksCodeSending() }
            if (error == null) {
                _state.update { it.copy(loading = false, info = RESET_CODE_SENT_MESSAGE, code = "") }
                startCountdown()
            } else {
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        val error = when {
            !isValidResetCode(s.code) -> "Escribe el código de $RESET_CODE_LENGTH dígitos que te llegó al correo."
            else -> newPasswordError(s.password, s.confirmPassword)
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            runCatching {
                if (!s.verified) {
                    auth.verifyResetCode(s.email, s.code)
                    _state.update { it.copy(verified = true) }
                }
                auth.updatePassword(s.password)
            }
                .onSuccess { _state.update { it.copy(loading = false, signedIn = true) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }

    /**
     * El código ya abrió una sesión pero el jugador se va sin cambiar la contraseña: se cierra esa
     * sesión igual que «Cerrar sesión», para no dejarlo dentro a medias.
     */
    fun abandon() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            auth.signOut()
            _state.update { it.copy(loading = false, signedOut = true) }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in RESEND_SECONDS downTo 1) {
                _state.update { it.copy(resendSeconds = seconds) }
                delay(1_000)
            }
            _state.update { it.copy(resendSeconds = 0) }
        }
    }
}
