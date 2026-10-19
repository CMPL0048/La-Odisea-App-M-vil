package com.laodisea.app.data.repository

import com.laodisea.app.data.local.PlayerCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionState {
    /** Hay sesión guardada; [offline] indica que no se pudo renovar el token por falta de red. */
    data class SignedIn(val userId: String?, val offline: Boolean) : SessionState
    data object SignedOut : SessionState
}

sealed interface SignUpResult {
    data object SignedIn : SignUpResult
    /** El proyecto pide confirmar el correo antes de iniciar sesión. */
    data object ConfirmEmail : SignUpResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val cache: PlayerCache,
) {
    private val auth get() = supabase.auth

    val currentUserId: String? get() = auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id

    val currentEmail: String? get() = auth.currentUserOrNull()?.email

    /** Espera a que la sesión guardada termine de cargarse y dice si el jugador sigue dentro. */
    suspend fun restoreSession(): SessionState =
        when (val status = auth.sessionStatus.first { it !is SessionStatus.Initializing }) {
            is SessionStatus.Authenticated -> SessionState.SignedIn(status.session.user?.id, offline = false)
            // Sin red no se puede renovar el token, pero la sesión sigue guardada: se entra con la caché.
            is SessionStatus.RefreshFailure -> SessionState.SignedIn(currentUserId ?: cache.ownerId(), offline = true)
            else -> SessionState.SignedOut
        }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        // Si entra otra cuenta, la caché anterior ya no le corresponde.
        if (cache.ownerId() != null && cache.ownerId() != currentUserId) cache.clear()
    }

    suspend fun signUp(displayName: String, email: String, password: String): SignUpResult {
        auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject { put("display_name", displayName.trim()) }
        }
        return if (auth.currentSessionOrNull() != null) SignUpResult.SignedIn else SignUpResult.ConfirmEmail
    }

    suspend fun signOut() {
        runCatching { auth.signOut() }
        cache.clear()
    }
}
