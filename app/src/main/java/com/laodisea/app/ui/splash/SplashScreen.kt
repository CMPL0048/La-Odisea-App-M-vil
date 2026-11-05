package com.laodisea.app.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.repository.AuthRepository
import com.laodisea.app.data.repository.SessionState
import com.laodisea.app.ui.components.LoadingPalette
import com.laodisea.app.ui.components.OdiseaLoadingScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _session = MutableStateFlow<SessionState?>(null)
    val session: StateFlow<SessionState?> = _session.asStateFlow()

    init {
        viewModelScope.launch {
            val minimum = launch { delay(800) } // deja ver el logo un instante
            val state = auth.restoreSession()
            minimum.join()
            _session.value = state
        }
    }
}

@Composable
fun SplashScreen(
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    LaunchedEffect(session) {
        when (session) {
            is SessionState.SignedIn -> onSignedIn()
            SessionState.SignedOut -> onSignedOut()
            null -> Unit
        }
    }
    // El logo ya trae el nombre, así que solo va el lema debajo con la paleta Bosque inicial.
    OdiseaLoadingScreen(
        message = "Una aventura por los campus de Nayarit",
        palette = LoadingPalette.Bosque,
    )
}
