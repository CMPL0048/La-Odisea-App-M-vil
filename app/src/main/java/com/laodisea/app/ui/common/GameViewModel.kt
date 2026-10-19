package com.laodisea.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.model.CatalogData
import com.laodisea.app.data.model.HomeData
import com.laodisea.app.data.repository.PlayerRepository
import com.laodisea.app.data.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lo que leen las pantallas de lectura: la foto del jugador, los catálogos y el estado de la red. */
data class GameUiState(
    val home: HomeData? = null,
    val catalog: CatalogData? = null,
    val sync: SyncStatus = SyncStatus(),
    /** Ya se leyó la caché local (aunque esté vacía). */
    val loaded: Boolean = false,
) {
    val snapshot get() = home?.snapshot
    /** Momento de los datos más viejos en pantalla, para el aviso sin conexión. */
    val fetchedAt: Long get() = listOfNotNull(home?.fetchedAt, catalog?.fetchedAt).minOrNull() ?: 0
}

fun GameUiState.screenStatus() = com.laodisea.app.ui.templates.ScreenStatus(
    sync = sync,
    hasData = home != null,
    loaded = loaded,
    fetchedAt = fetchedAt,
)

/**
 * ViewModel común de Estadísticas, Mapa, Mochila, Muestrario y Bestiario: todas leen de la misma
 * caché (DataStore), que se renueva con [PlayerRepository.sync].
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: PlayerRepository,
) : ViewModel() {

    val state: StateFlow<GameUiState> = combine(
        repository.cachedHome,
        repository.cachedCatalog,
        repository.syncStatus,
    ) { home, catalog, sync -> GameUiState(home, catalog, sync, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState())

    init {
        // Solo se descarga si falta algo o si lo guardado tiene más de un minuto.
        viewModelScope.launch {
            val home = repository.cachedHome.first()
            val catalog = repository.cachedCatalog.first()
            val oldest = minOf(home?.fetchedAt ?: 0, catalog?.fetchedAt ?: 0)
            if (System.currentTimeMillis() - oldest > STALE_AFTER_MS) repository.sync()
        }
    }

    fun refresh() {
        viewModelScope.launch { repository.sync() }
    }

    private companion object {
        const val STALE_AFTER_MS = 60_000L
    }
}
