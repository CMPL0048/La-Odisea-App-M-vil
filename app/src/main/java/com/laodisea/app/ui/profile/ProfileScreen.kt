package com.laodisea.app.ui.profile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.domain.AvatarRef
import com.laodisea.app.domain.parseAvatarUrl
import com.laodisea.app.ui.auth.AuthTextField
import com.laodisea.app.ui.auth.ErrorText
import com.laodisea.app.ui.auth.PrimaryButton
import com.laodisea.app.ui.components.AvatarPresets
import com.laodisea.app.ui.components.LoadingPalette
import com.laodisea.app.ui.components.OdiseaAvatar
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.components.OdiseaLoadingOverlay
import com.laodisea.app.ui.components.PresetAvatar
import com.laodisea.app.ui.templates.FormScreenTemplate

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    savesViewModel: SavesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saves by savesViewModel.list.collectAsStateWithLifecycle()
    val savesState by savesViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var lastSaveMessage by remember { mutableStateOf("Actualizando partida…") }
    savesState.loadingMessage?.let { lastSaveMessage = it }

    LaunchedEffect(savesState.message) {
        savesState.message?.let {
            snackbar.showSnackbar(it)
            savesViewModel.onMessageShown()
        }
    }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val avatarUrl = profile?.avatarUrl
    var showPicker by rememberSaveable { mutableStateOf(false) }
    // El selector de fotos del sistema no necesita permisos de almacenamiento.
    val pickPhoto = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            showPicker = false
            viewModel.onPhotoPicked(uri)
        }
    }

    Box(Modifier.fillMaxSize()) {
        FormScreenTemplate(title = "Perfil", onBack = onBack, snackbarHostState = snackbar) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    OdiseaAvatar(avatarUrl = avatarUrl, displayName = profile?.displayName, size = 120.dp)
                    if (state.avatarBusy) CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
                OutlinedButton(onClick = { showPicker = true }, enabled = !state.avatarBusy) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("Cambiar", modifier = Modifier.padding(start = 8.dp))
                }
            }
            ErrorText(state.avatarError)
            OdiseaCard(title = "Nombre de jugador", icon = Icons.Outlined.Person) {
                AuthTextField(
                    value = state.displayName,
                    onValueChange = viewModel::onNameChange,
                    label = "Nombre de jugador",
                    enabled = !state.savingName,
                    imeAction = ImeAction.Done,
                    onDone = viewModel::saveName,
                )
                Text(
                    "Así te verán en la app y en el juego de PC.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorText(state.nameError)
                if (state.nameSaved) {
                    Text("Nombre guardado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                PrimaryButton(
                    text = "Guardar nombre",
                    onClick = viewModel::saveName,
                    enabled = viewModel.canSaveName,
                    loading = state.savingName,
                )
            }
            OdiseaCard(title = "Correo", icon = Icons.Outlined.Email) {
                Text(viewModel.email ?: "Sin correo", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Es el correo con el que entras a la app y al juego de PC.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Sin la migración 08 en el servidor no hay lista: la sección no aparece.
            if (saves.isNotEmpty()) SavesSection(saves = saves, state = savesState, viewModel = savesViewModel)
        }
        OdiseaLoadingOverlay(
            visible = savesState.loadingMessage != null,
            message = lastSaveMessage,
            subtitle = "Sincronizando progreso y recompensas",
            palette = LoadingPalette.Travesia,
            compact = true,
        )
    }

    if (showPicker) {
        AvatarPickerSheet(
            avatarUrl = avatarUrl,
            onDismiss = { showPicker = false },
            onSelectPreset = { key ->
                showPicker = false
                viewModel.selectPreset(key)
            },
            onUploadPhoto = { pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            onRemovePhoto = {
                showPicker = false
                viewModel.removePhoto()
            },
        )
    }

    state.pendingPhoto?.let { photo ->
        PhotoPreviewDialog(photo = photo, onConfirm = viewModel::confirmPhoto, onDismiss = viewModel::discardPhoto)
    }
}

/** Hoja con los avatares precargados, subir foto y quitar la foto propia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarPickerSheet(
    avatarUrl: String?,
    onDismiss: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onUploadPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val current = remember(avatarUrl) { parseAvatarUrl(avatarUrl) }
    // Sin avatar elegido se ve Víctor, así que también se marca como el actual.
    val selectedKey = when (current) {
        is AvatarRef.Preset -> current.key
        AvatarRef.Default -> AvatarPresets.default.key
        is AvatarRef.Photo -> null
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Elige tu avatar", style = MaterialTheme.typography.titleLarge)
            AvatarPresets.all.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { preset ->
                        val selected = preset.key == selectedKey
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(role = Role.Button, onClick = { onSelectPreset(preset.key) })
                                .semantics { this.selected = selected }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            PresetAvatar(
                                preset = preset,
                                size = 64.dp,
                                modifier = if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            Text(
                                if (selected) "${preset.name} ✓" else preset.name,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    // Completa la última fila para que las columnas no se estiren.
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            OutlinedButton(onClick = onUploadPhoto, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Text("Subir foto de la galería", modifier = Modifier.padding(start = 8.dp))
            }
            if (current is AvatarRef.Photo) {
                TextButton(onClick = onRemovePhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text("Quitar foto", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Vista previa de la foto ya recortada antes de subirla. */
@Composable
private fun PhotoPreviewDialog(photo: PendingPhoto, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val bitmap = remember(photo) { BitmapFactory.decodeByteArray(photo.webp, 0, photo.webp.size)?.asImageBitmap() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vista previa") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Vista previa de tu foto",
                        modifier = Modifier.size(160.dp).clip(CircleShape),
                    )
                }
                Text("Así se verá tu avatar en la app.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Usar esta foto") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
