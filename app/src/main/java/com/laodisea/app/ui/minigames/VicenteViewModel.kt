package com.laodisea.app.ui.minigames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.model.CatalogItem
import com.laodisea.app.data.model.MinigameStatus
import com.laodisea.app.data.repository.MinigameRepository
import com.laodisea.app.data.repository.MinigameReward
import com.laodisea.app.data.repository.PlayerRepository
import com.laodisea.app.data.toUserMessage
import com.laodisea.app.domain.minigames.Negotiation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VicenteUiState(
    val negotiation: Negotiation? = null,
    val offerText: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val reward: MinigameReward? = null,
)

/** Datos de la foto del jugador que usa la pantalla: saldo, intentos y catálogo de objetos. */
data class VicenteContext(
    val coins: Long = 0,
    val status: MinigameStatus? = null,
    val items: Map<String, CatalogItem> = emptyMap(),
)

@HiltViewModel
class VicenteViewModel @Inject constructor(
    private val minigames: MinigameRepository,
    players: PlayerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VicenteUiState())
    val state: StateFlow<VicenteUiState> = _state.asStateFlow()

    val context: StateFlow<VicenteContext> = combine(players.cachedHome, players.cachedCatalog) { home, catalog ->
        VicenteContext(
            coins = home?.snapshot?.coins ?: 0,
            status = home?.minigames?.firstOrNull { it.code == MinigameRepository.CODE_VICENTE },
            items = catalog?.items.orEmpty().associateBy { it.code },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VicenteContext())


    /** Abre la negociación (gasta un intento) o retoma la que quedó abierta. */
    fun start() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { minigames.startNegotiation() }
                .onSuccess { n -> _state.update { it.copy(busy = false, negotiation = n) } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUserMessage()) } }
        }
    }

    fun onOfferChange(value: String) =
        _state.update { it.copy(offerText = value.filter(Char::isDigit).take(6), error = null) }

    fun useSuggestion(amount: Long) = _state.update { it.copy(offerText = amount.toString(), error = null) }

    fun sendOffer() {
        val s = _state.value
        val negotiation = s.negotiation ?: return
        if (s.busy) return
        val offer = s.offerText.toLongOrNull()
        negotiation.validateOffer(offer, context.value.coins)?.let { error ->
            _state.update { it.copy(error = error.message) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { minigames.makeOffer(negotiation.id, offer!!) }
                .onSuccess { response ->
                    val updated = negotiation.apply(offer!!, response)
                    val reward = when (updated.status) {
                        Negotiation.Status.Aceptada -> MinigameReward(true, -(updated.finalPrice ?: offer), updated.itemCode)
                        Negotiation.Status.SeRetiro -> MinigameReward(false, 0, null)
                        Negotiation.Status.Abierta -> null
                    }
                    _state.update { it.copy(busy = false, negotiation = updated, offerText = "", reward = reward) }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUserMessage()) } }
        }
    }
}
