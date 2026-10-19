package com.laodisea.app.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.laodisea.app.R
import com.laodisea.app.data.repository.AuthRepository
import com.laodisea.app.data.repository.SessionState
import com.laodisea.app.ui.theme.BosqueProfundo
import com.laodisea.app.ui.theme.OroMoneda
import com.laodisea.app.ui.theme.Papel
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
    Box(
        modifier = Modifier.fillMaxSize().background(BosqueProfundo),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(160.dp),
            )
            Text("La Odisea", style = MaterialTheme.typography.displaySmall, color = OroMoneda)
            Text("Una aventura por los campus de Nayarit", style = MaterialTheme.typography.bodyMedium, color = Papel)
            CircularProgressIndicator(color = OroMoneda, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}
