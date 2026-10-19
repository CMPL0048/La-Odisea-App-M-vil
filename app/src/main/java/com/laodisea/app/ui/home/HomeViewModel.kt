package com.laodisea.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.model.HomeData
import com.laodisea.app.data.repository.PlayerRepository
import com.laodisea.app.data.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val data: HomeData? = null,
    val sync: SyncStatus = SyncStatus(),
) {
    val refreshing: Boolean get() = sync.refreshing
    val offline: Boolean get() = sync.offline
    val error: String? get() = sync.error
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PlayerRepository,
) : ViewModel() {

    // Primero lo guardado (aparece al instante y sirve sin conexión); luego se pide lo nuevo.
    val state: StateFlow<HomeUiState> = combine(repository.cachedHome, repository.syncStatus, ::HomeUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(sync = SyncStatus(refreshing = true)))

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repository.sync() }
    }
}
