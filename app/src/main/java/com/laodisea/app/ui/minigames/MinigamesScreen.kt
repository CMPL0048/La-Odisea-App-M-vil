package com.laodisea.app.ui.minigames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.model.MinigameStatus
import com.laodisea.app.data.repository.MinigameRepository
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.common.screenStatus
import com.laodisea.app.ui.components.AttemptDots
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.templates.ListScreenTemplate
import com.laodisea.app.ui.util.Formats

/** Textos de cada minijuego (el catálogo del servidor solo trae código y nombre). */
object MinigameInfo {
    fun description(code: String) = when (code) {
        MinigameRepository.CODE_SALVA ->
            "Salva se quedó dormido otra vez. Toca justo cuando el marcador pase por la zona dorada para despertarlo y gana monedas."
        MinigameRepository.CODE_VICENTE ->
            "Vicente el Águila vende objetos para la odisea. Regatea sin agotar su paciencia y llévatelo más barato."
        else -> "Minijuego de La Odisea."
    }

    fun icon(code: String): ImageVector = when (code) {
        MinigameRepository.CODE_SALVA -> Icons.Outlined.Alarm
        MinigameRepository.CODE_VICENTE -> Icons.Outlined.Handshake
        else -> Icons.Outlined.SportsEsports
    }
}

@Composable
fun MinigamesScreen(
    onBack: () -> Unit,
    onPlay: (code: String) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val home = state.home
    val offline = state.sync.offline

    ListScreenTemplate(
        title = "Minijuegos",
        subtitle = "4 intentos cada 4 horas por minijuego",
        status = state.screenStatus(),
        onRefresh = viewModel::refresh,
        onBack = onBack,
        coins = home?.snapshot?.coins,
    ) {
        items(home?.minigames.orEmpty(), key = { it.code }) { game ->
            MinigameCard(game, offline = offline, onPlay = { onPlay(game.code) })
        }
    }
}

@Composable
private fun MinigameCard(game: MinigameStatus, offline: Boolean, onPlay: () -> Unit) {
    val hasAttempts = game.attemptsRemaining > 0
    OdiseaCard(title = game.name, icon = MinigameInfo.icon(game.code)) {
        Text(MinigameInfo.description(game.code), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AttemptDots(game.attemptsRemaining, game.attemptsPerCycle)
                if (game.attemptsRemaining < game.attemptsPerCycle) {
                    Text(
                        "Se recargan ${Formats.untilReset(game.nextResetAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onPlay,
                enabled = hasAttempts && !offline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) { Text(if (offline) "Sin conexión" else if (hasAttempts) "Jugar" else "Sin intentos") }
        }
    }
}

