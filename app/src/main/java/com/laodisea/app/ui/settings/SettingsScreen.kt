package com.laodisea.app.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.laodisea.app.data.repository.PlayerRepository
import com.laodisea.app.data.repository.SettingsRepository
import com.laodisea.app.data.repository.ThemePreference
import com.laodisea.app.data.toUserMessage
import com.laodisea.app.ui.auth.AuthTextField
import com.laodisea.app.ui.auth.ErrorText
import com.laodisea.app.ui.auth.PrimaryButton
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.templates.FormScreenTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileForm(
    val displayName: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val players: PlayerRepository,
) : ViewModel() {

    val theme: StateFlow<ThemePreference> = settings.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference.Sistema)

    private val _profile = MutableStateFlow(ProfileForm())
    val profile: StateFlow<ProfileForm> = _profile.asStateFlow()

    private var currentName: String = ""

    init {
        viewModelScope.launch {
            currentName = players.cachedHome.filterNotNull().map { it.snapshot.player?.displayName.orEmpty() }.first()
            _profile.update { if (it.displayName.isEmpty()) it.copy(displayName = currentName) else it }
        }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun onNameChange(value: String) = _profile.update { it.copy(displayName = value.take(24), error = null, saved = false) }

    val canSave: Boolean
        get() = _profile.value.let { !it.saving && it.displayName.trim() != currentName }

    fun saveName() {
        val name = _profile.value.displayName.trim()
        if (name.length < 3) {
            _profile.update { it.copy(error = "El nombre de jugador debe tener al menos 3 letras.") }
            return
        }
        _profile.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { players.updateDisplayName(name) }
                .onSuccess {
                    currentName = name
                    _profile.update { it.copy(saving = false, saved = true) }
                }
                .onFailure { e -> _profile.update { it.copy(saving = false, error = e.toUserMessage()) } }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    FormScreenTemplate(title = "Ajustes y perfil", onBack = onBack) {
        OdiseaCard(title = "Perfil", icon = Icons.Outlined.Person) {
            AuthTextField(
                value = profile.displayName,
                onValueChange = viewModel::onNameChange,
                label = "Nombre de jugador",
                enabled = !profile.saving,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                onDone = viewModel::saveName,
            )
            Text(
                "Así te verán en la app y en el juego de PC.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ErrorText(profile.error)
            if (profile.saved) {
                Text("Nombre guardado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            PrimaryButton(
                text = "Guardar nombre",
                onClick = viewModel::saveName,
                enabled = viewModel.canSave,
                loading = profile.saving,
            )
        }
        OdiseaCard(title = "Tema", icon = Icons.Outlined.Palette) {
            androidx.compose.foundation.layout.Column(Modifier.selectableGroup()) {
                ThemePreference.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == theme, onClick = { viewModel.setTheme(option) }, role = Role.RadioButton)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == theme, onClick = null)
                        Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }
}
