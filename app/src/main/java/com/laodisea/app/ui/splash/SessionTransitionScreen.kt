package com.laodisea.app.ui.splash

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.repository.PlayerRepository
import com.laodisea.app.ui.components.LoadingPalette
import com.laodisea.app.ui.components.OdiseaLoadingScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Pantalla intermedia de carga al iniciar o cerrar sesión.
 * Al entrar precarga en paralelo la foto de la partida activa; al salir muestra una transición breve.
 */
@HiltViewModel
class SessionTransitionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val players: PlayerRepository,
) : ViewModel() {

    val signingIn: Boolean = savedStateHandle.get<Boolean>("signingIn") ?: true

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            if (signingIn) {
                val minimum = launch { delay(750) }
                // Si la red tarda demasiado no se bloquea la entrada; Inicio seguirá sincronizando.
                withTimeoutOrNull(2_500) { players.sync() }
                minimum.join()
            } else {
                delay(650)
            }
            _ready.value = true
        }
    }
}

@Composable
fun SessionTransitionScreen(
    onFinished: () -> Unit,
    viewModel: SessionTransitionViewModel = hiltViewModel(),
) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    BackHandler(enabled = true) { /* Transición breve en curso */ }
    LaunchedEffect(ready) {
        if (ready) onFinished()
    }
    if (viewModel.signingIn) {
        OdiseaLoadingScreen(
            message = "Preparando tu odisea…",
            subtitle = "Sincronizando tu progreso con el campus",
            palette = LoadingPalette.Travesia,
        )
    } else {
        OdiseaLoadingScreen(
            message = "Cerrando sesión…",
            subtitle = "Hasta pronto, aventurero",
            palette = LoadingPalette.Despedida,
        )
    }
}
