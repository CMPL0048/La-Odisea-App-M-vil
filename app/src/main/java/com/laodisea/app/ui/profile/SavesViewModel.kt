package com.laodisea.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.model.SaveSummary
import com.laodisea.app.data.repository.SavesRepository
import com.laodisea.app.data.toUserMessage
import com.laodisea.app.domain.LinkCodeResult
import com.laodisea.app.domain.MAX_SAVES
import com.laodisea.app.domain.parseLinkCode
import com.laodisea.app.domain.saveNameError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavesUiState(
    val busy: Boolean = false,
    /** Mensaje para la pantalla de carga completa al cambiar o actualizar la partida activa. */
    val loadingMessage: String? = null,
    /** Error general de la sección (cambiar, renombrar, desvincular, escáner). */
    val error: String? = null,
    /** Error dentro del diálogo «Escribir código». */
    val codeError: String? = null,
    /** Error dentro del diálogo «Renombrar». */
    val renameError: String? = null,
    /** Aviso de un momento (snackbar), por ejemplo «Partida vinculada: …». */
    val message: String? = null,
    /** Se cerró bien un diálogo (código o renombrar); la pantalla lo cierra y lo consume. */
    val dialogDone: Boolean = false,
)

/** Partidas vinculadas: lo usan Perfil (todo) e Inicio (solo cambiar de partida). */
@HiltViewModel
class SavesViewModel @Inject constructor(
    private val saves: SavesRepository,
) : ViewModel() {

    val list: StateFlow<List<SaveSummary>> = saves.saves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(SavesUiState())
    val state: StateFlow<SavesUiState> = _state.asStateFlow()

    val canAddMore: Boolean get() = list.value.size < MAX_SAVES

    fun activate(save: SaveSummary) {
        if (save.isActive) return
        run(
            loadingMessage = "Cambiando a ${save.name}…",
            onSuccess = { it.copy(message = "Ahora ves: ${save.name}") },
        ) { saves.setActive(save.saveId) }
    }

    fun rename(save: SaveSummary, name: String) {
        saveNameError(name)?.let { error ->
            _state.update { it.copy(renameError = error) }
            return
        }
        run(
            onSuccess = { it.copy(dialogDone = true) },
            onError = { s, message -> s.copy(renameError = message) },
        ) { saves.rename(save.saveId, name) }
    }

    fun unlink(save: SaveSummary) {
        run(
            loadingMessage = "Desvinculando ${save.name}…",
            onSuccess = { it.copy(message = "Se desvinculó ${save.name}") },
        ) { saves.unlink(save.saveId) }
    }

    /** Vincula con lo escaneado o escrito; [fromDialog] decide dónde se muestra el error. */
    fun link(raw: String, fromDialog: Boolean) {
        when (val parsed = parseLinkCode(raw)) {
            is LinkCodeResult.Invalid -> _state.update {
                if (fromDialog) it.copy(codeError = parsed.message) else it.copy(error = parsed.message)
            }
            is LinkCodeResult.Valid -> run(
                loadingMessage = if (fromDialog) null else "Vinculando partida…",
                onSuccess = { it },
                onError = { s, message -> if (fromDialog) s.copy(codeError = message) else s.copy(error = message) },
            ) {
                val linked = saves.link(parsed.code)
                val device = linked.deviceName?.takeIf { d -> d.isNotBlank() }?.let { d -> " ($d)" }.orEmpty()
                _state.update { it.copy(message = "Partida vinculada: ${linked.name}$device", dialogDone = true) }
            }
        }
    }

    fun showError(message: String) = _state.update { it.copy(error = message) }
    fun clearDialogErrors() = _state.update { it.copy(codeError = null, renameError = null) }
    fun onMessageShown() = _state.update { it.copy(message = null) }
    fun onDialogDoneHandled() = _state.update { it.copy(dialogDone = false) }

    private fun run(
        loadingMessage: String? = null,
        onSuccess: (SavesUiState) -> SavesUiState,
        onError: (SavesUiState, String) -> SavesUiState = { s, message -> s.copy(error = message) },
        action: suspend () -> Unit,
    ) {
        if (_state.value.busy) return
        _state.update {
            it.copy(busy = true, loadingMessage = loadingMessage, error = null, codeError = null, renameError = null)
        }
        viewModelScope.launch {
            runCatching {
                if (loadingMessage != null) {
                    coroutineScope {
                        val minimum = launch { delay(650) }
                        action()
                        minimum.join()
                    }
                } else {
                    action()
                }
            }
                .onSuccess { _state.update { onSuccess(it.copy(busy = false, loadingMessage = null)) } }
                .onFailure { e -> _state.update { onError(it.copy(busy = false, loadingMessage = null), e.toUserMessage()) } }
        }
    }
}
