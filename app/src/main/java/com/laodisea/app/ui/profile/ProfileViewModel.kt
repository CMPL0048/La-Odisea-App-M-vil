package com.laodisea.app.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.AvatarImageProcessor
import com.laodisea.app.data.model.PlayerProfile
import com.laodisea.app.data.repository.ProfileRepository
import com.laodisea.app.data.toUserMessage
import com.laodisea.app.domain.displayNameError
import com.laodisea.app.domain.limitDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Foto ya recortada y comprimida que espera a que el jugador toque «Usar esta foto». */
class PendingPhoto(val webp: ByteArray)

data class ProfileUiState(
    val displayName: String = "",
    val savingName: Boolean = false,
    val nameError: String? = null,
    val nameSaved: Boolean = false,
    /** Se está procesando o subiendo la foto, o cambiando el avatar. */
    val avatarBusy: Boolean = false,
    val avatarError: String? = null,
    val pendingPhoto: PendingPhoto? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val images: AvatarImageProcessor,
) : ViewModel() {

    val profile: StateFlow<PlayerProfile?> = profiles.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val email: String? get() = profiles.email

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var currentName: String = ""

    init {
        viewModelScope.launch {
            currentName = profiles.profile.filterNotNull().first().displayName.orEmpty()
            _state.update { if (it.displayName.isEmpty()) it.copy(displayName = currentName) else it }
        }
    }

    fun onNameChange(value: String) =
        _state.update { it.copy(displayName = limitDisplayName(value), nameError = null, nameSaved = false) }

    val canSaveName: Boolean
        get() = _state.value.let { !it.savingName && it.displayName.trim() != currentName }

    fun saveName() {
        val name = _state.value.displayName.trim()
        displayNameError(name)?.let { error ->
            _state.update { it.copy(nameError = error) }
            return
        }
        _state.update { it.copy(savingName = true, nameError = null) }
        viewModelScope.launch {
            runCatching { profiles.updateDisplayName(name) }
                .onSuccess {
                    currentName = name
                    _state.update { it.copy(savingName = false, nameSaved = true) }
                }
                .onFailure { e -> _state.update { it.copy(savingName = false, nameError = e.toUserMessage()) } }
        }
    }

    fun selectPreset(key: String) = changeAvatar { profiles.setPresetAvatar(key) }

    fun removePhoto() = changeAvatar { profiles.clearAvatar() }

    /** Prepara la foto elegida en la galería y la deja en vista previa. */
    fun onPhotoPicked(uri: Uri) {
        _state.update { it.copy(avatarBusy = true, avatarError = null) }
        viewModelScope.launch {
            runCatching { images.process(uri) }
                .onSuccess { webp -> _state.update { it.copy(avatarBusy = false, pendingPhoto = PendingPhoto(webp)) } }
                .onFailure { e -> _state.update { it.copy(avatarBusy = false, avatarError = e.toUserMessage()) } }
        }
    }

    fun confirmPhoto() {
        val photo = _state.value.pendingPhoto ?: return
        _state.update { it.copy(pendingPhoto = null) }
        changeAvatar { profiles.uploadAvatar(photo.webp) }
    }

    fun discardPhoto() = _state.update { it.copy(pendingPhoto = null) }

    private fun changeAvatar(action: suspend () -> Unit) {
        if (_state.value.avatarBusy) return
        _state.update { it.copy(avatarBusy = true, avatarError = null) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { _state.update { it.copy(avatarBusy = false) } }
                .onFailure { e -> _state.update { it.copy(avatarBusy = false, avatarError = e.toUserMessage()) } }
        }
    }
}
