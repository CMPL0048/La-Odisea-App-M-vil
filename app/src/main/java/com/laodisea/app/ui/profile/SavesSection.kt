package com.laodisea.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.laodisea.app.data.model.SaveSummary
import com.laodisea.app.domain.LINK_CODE_LENGTH
import com.laodisea.app.domain.MAX_SAVES
import com.laodisea.app.domain.SAVE_NAME_MAX_LENGTH
import com.laodisea.app.domain.normalizeLinkCode
import com.laodisea.app.ui.auth.ErrorText
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.components.OdiseaChip
import com.laodisea.app.ui.util.Formats
import kotlinx.coroutines.launch

/**
 * «Mis partidas (n/5)» de Perfil: la principal y las vinculadas, cambiar la activa, renombrar,
 * desvincular y agregar con QR o escribiendo el código.
 */
@Composable
fun SavesSection(
    saves: List<SaveSummary>,
    state: SavesUiState,
    viewModel: SavesViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Diálogos abiertos (por id de partida para que sobrevivan a girar la pantalla).
    var confirmId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var unlinkId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCodeDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.dialogDone) {
        if (state.dialogDone) {
            showCodeDialog = false
            renameId = null
            viewModel.onDialogDoneHandled()
        }
    }

    val full = saves.size >= MAX_SAVES
    OdiseaCard(title = "Mis partidas (${saves.size}/$MAX_SAVES)", icon = Icons.Outlined.SportsEsports) {
        saves.forEach { save ->
            SaveCard(
                save = save,
                enabled = !state.busy,
                onClick = { if (!save.isActive) confirmId = save.saveId },
                onRename = { viewModel.clearDialogErrors(); renameId = save.saveId },
                onUnlink = { unlinkId = save.saveId },
            )
        }
        ErrorText(state.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        when (val result = scanLinkQr(context)) {
                            is QrScanResult.Scanned -> viewModel.link(result.text, fromDialog = false)
                            QrScanResult.Cancelled -> Unit
                            is QrScanResult.Unavailable -> {
                                viewModel.showError(result.message)
                                viewModel.clearDialogErrors()
                                showCodeDialog = true
                            }
                        }
                    }
                },
                enabled = !full && !state.busy,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Text("Escanear QR", modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = { viewModel.clearDialogErrors(); showCodeDialog = true },
                enabled = !full && !state.busy,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Keyboard, contentDescription = null)
                Text("Escribir código", modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            if (full) {
                "Desvincula una partida para agregar otra."
            } else {
                "Juega en la PC sin cuenta y escanea el QR que aparece en el juego para traer esa partida aquí."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    saves.firstOrNull { it.saveId == confirmId }?.let { save ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text("Cambiar de partida") },
            text = { Text("¿Ver y jugar minijuegos con ${save.name}?") },
            confirmButton = {
                TextButton(onClick = { confirmId = null; viewModel.activate(save) }) { Text("Cambiar") }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("Cancelar") } },
        )
    }
    saves.firstOrNull { it.saveId == renameId }?.let { save ->
        RenameDialog(
            save = save,
            error = state.renameError,
            busy = state.busy,
            onConfirm = { name -> viewModel.rename(save, name) },
            onDismiss = { renameId = null },
        )
    }
    saves.firstOrNull { it.saveId == unlinkId }?.let { save ->
        AlertDialog(
            onDismissRequest = { unlinkId = null },
            title = { Text("¿Desvincular ${save.name}?") },
            text = { Text("La partida seguirá en la PC y podrás vincularla otra vez.") },
            confirmButton = {
                TextButton(onClick = { unlinkId = null; viewModel.unlink(save) }) { Text("Desvincular") }
            },
            dismissButton = { TextButton(onClick = { unlinkId = null }) { Text("Cancelar") } },
        )
    }
    if (showCodeDialog) {
        LinkCodeDialog(
            error = state.codeError,
            busy = state.busy,
            onConfirm = { code -> viewModel.link(code, fromDialog = true) },
            onDismiss = { showCodeDialog = false },
        )
    }
}

/** Tarjeta de una partida: nombre, campus, nivel, tiempo jugado y última vez. */
@Composable
fun SaveCard(
    save: SaveSummary,
    enabled: Boolean,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onUnlink: (() -> Unit)? = null,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = if (save.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            if (save.isActive) 2.dp else 1.dp,
            if (save.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth().semantics { selected = save.isActive },
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(save.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                    if (save.isActive) OdiseaChip("Activa")
                }
                val place = listOfNotNull(save.zoneName, save.level?.let { "Nivel $it" }).joinToString(" · ")
                if (place.isNotEmpty()) Text(place, style = MaterialTheme.typography.bodyMedium)
                val played = listOfNotNull(
                    save.playtimeSeconds?.let { "${Formats.playtime(it)} jugado" },
                    Formats.timeAgo(save.lastPlayed)?.let { "Última vez $it" },
                ).joinToString(" · ")
                if (played.isNotEmpty()) {
                    Text(played, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // La principal no se renombra ni se desvincula.
            if (!save.isPrincipal && onRename != null && onUnlink != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }, enabled = enabled) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Opciones de ${save.name}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Renombrar") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("Desvincular") },
                            leadingIcon = { Icon(Icons.Outlined.LinkOff, contentDescription = null) },
                            onClick = { menuOpen = false; onUnlink() },
                        )
                    }
                }
            }
        }
    }
}

/** Hoja de Inicio para cambiar de partida (solo selección). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePickerSheet(
    saves: List<SaveSummary>,
    busy: Boolean,
    onSelect: (SaveSummary) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Elige la partida", style = MaterialTheme.typography.titleLarge)
            Text(
                "Lo que ves en la app y los premios de los minijuegos son de la partida activa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            saves.forEach { save -> SaveCard(save = save, enabled = !busy, onClick = { onSelect(save) }) }
        }
    }
}

@Composable
private fun RenameDialog(
    save: SaveSummary,
    error: String?,
    busy: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(save.saveId) { mutableStateOf(save.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar partida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(SAVE_NAME_MAX_LENGTH) },
                    label = { Text("Nombre (hasta $SAVE_NAME_MAX_LENGTH)") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                ErrorText(error)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = !busy) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun LinkCodeDialog(
    error: String?,
    busy: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escribir código") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Escribe el código de $LINK_CODE_LENGTH caracteres que aparece debajo del QR en el juego.")
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = normalizeLinkCode(it).take(LINK_CODE_LENGTH) },
                    label = { Text("Código") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(code) }, enabled = !busy && code.length == LINK_CODE_LENGTH) { Text("Vincular") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
