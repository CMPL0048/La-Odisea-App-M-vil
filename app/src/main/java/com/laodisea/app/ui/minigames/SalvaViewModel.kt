package com.laodisea.app.ui.minigames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.model.MinigameStatus
import com.laodisea.app.data.repository.MinigameRepository
import com.laodisea.app.data.repository.MinigameReward
import com.laodisea.app.data.repository.PlayerRepository
import com.laodisea.app.data.toUserMessage
import com.laodisea.app.domain.minigames.SalvaGame
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalvaUiState(
    val game: SalvaGame.State = SalvaGame.State(),
    val submitting: Boolean = false,
    val error: String? = null,
    /** Respuesta del servidor; cuando llega, la pantalla pasa a la de recompensa. */
    val reward: MinigameReward? = null,
)

@HiltViewModel
class SalvaViewModel @Inject constructor(
    private val minigames: MinigameRepository,
    players: PlayerRepository,
) : ViewModel() {

    private val engine = SalvaGame()

    private val _state = MutableStateFlow(SalvaUiState())
    val state: StateFlow<SalvaUiState> = _state.asStateFlow()

    val status: StateFlow<MinigameStatus?> = players.cachedHome
        .map { home -> home?.minigames?.firstOrNull { it.code == MinigameRepository.CODE_SALVA } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val coins: StateFlow<Long?> = players.cachedHome
        .map { it?.snapshot?.coins }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun start() {
        engine.start()
        publish()
    }

    fun tick(seconds: Float) {
        engine.tick(seconds)
        publish()
    }

    fun tap(): SalvaGame.TapResult {
        val result = engine.tap()
        publish()
        if (engine.state.finished) submit()
        return result
    }

    /** Salir a media partida cuenta como intento fallido. */
    fun abandon() {
        engine.abandon()
        publish()
        submit()
    }

    /** Manda el resultado al servidor; se puede reintentar si falló la red. */
    fun submit() {
        val s = _state.value
        if (s.submitting || s.reward != null || !engine.state.finished) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            runCatching { minigames.playSalva(success = engine.state.won) }
                .onSuccess { reward -> _state.update { it.copy(submitting = false, reward = reward) } }
                .onFailure { e -> _state.update { it.copy(submitting = false, error = e.toUserMessage()) } }
        }
    }

    private fun publish() = _state.update { it.copy(game = engine.state) }
}
