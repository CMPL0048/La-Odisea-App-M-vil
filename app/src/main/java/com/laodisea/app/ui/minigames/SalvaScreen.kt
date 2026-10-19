package com.laodisea.app.ui.minigames

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.repository.MinigameReward
import com.laodisea.app.domain.minigames.SalvaGame
import com.laodisea.app.ui.auth.ErrorText
import com.laodisea.app.ui.auth.PrimaryButton
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.templates.MinigameScreenTemplate
import com.laodisea.app.ui.theme.BosqueProfundo
import com.laodisea.app.ui.theme.OroMoneda
import com.laodisea.app.ui.theme.Vida
import kotlinx.coroutines.isActive

@Composable
fun SalvaScreen(
    onBack: () -> Unit,
    onFinished: (MinigameReward) -> Unit,
    viewModel: SalvaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val coins by viewModel.coins.collectAsStateWithLifecycle()
    val game = state.game
    val playing = game.phase == SalvaGame.Phase.Jugando
    var confirmExit by remember { mutableStateOf(false) }

    LaunchedEffect(state.reward) { state.reward?.let(onFinished) }

    // Mueve el marcador en cada cuadro mientras se juega.
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                viewModel.tick((now - last) / 1_000_000_000f)
                last = now
            }
        }
    }

    BackHandler(enabled = playing || state.submitting) { if (playing) confirmExit = true }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Dejar a Salva dormido?") },
            text = { Text("Si sales ahora, el intento cuenta como fallido.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; viewModel.abandon() }) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Seguir jugando") } },
        )
    }

    MinigameScreenTemplate(
        title = "Despierta a Salva",
        onBack = { if (playing) confirmExit = true else if (!state.submitting) onBack() },
        coins = coins,
        attemptsRemaining = status?.attemptsRemaining,
        attemptsTotal = status?.attemptsPerCycle,
    ) {
        SalvaPortrait(game)
        when (game.phase) {
            SalvaGame.Phase.Listo -> Intro(
                canPlay = (status?.attemptsRemaining ?: 0) > 0,
                onStart = viewModel::start,
            )
            SalvaGame.Phase.Jugando -> PlayArea(game, onTap = viewModel::tap)
            else -> Submitting(state, onRetry = viewModel::submit)
        }
    }
}

@Composable
private fun SalvaPortrait(game: SalvaGame.State) {
    val awake by animateFloatAsState(game.wakefulness, label = "awake")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 168.dp, height = 128.dp)) {
            Box(
                Modifier.size(120.dp).align(Alignment.Center).clip(CircleShape).background(BosqueProfundo),
                contentAlignment = Alignment.Center,
            ) {
                Text("S", style = MaterialTheme.typography.displaySmall, color = OroMoneda)
            }
            // Los «Zzz» se desvanecen a medida que Salva despierta.
            Text(
                "Zzz",
                style = MaterialTheme.typography.titleLarge,
                color = OroMoneda,
                modifier = Modifier.align(Alignment.TopEnd).alpha(1f - awake),
            )
        }
        Text(
            when {
                game.won -> "¡Salva despertó!"
                game.phase == SalvaGame.Phase.Perdido -> "Salva sigue roncando…"
                game.hits == 0 -> "Salva está profundamente dormido"
                game.hits == 1 -> "Salva abrió un ojo…"
                else -> "¡Ya casi despierta!"
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ColumnScope.Intro(canPlay: Boolean, onStart: () -> Unit) {
    OdiseaCard(title = "Cómo se juega") {
        Text(
            "El marcador va y viene por la barra. Toca la pantalla cuando pase por la zona dorada.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Con ${SalvaGame.HITS_TO_WIN} aciertos Salva despierta; con ${SalvaGame.MAX_MISSES} fallos se queda dormido. " +
                "Cada acierto lo hace más difícil. Las monedas las da el servidor y bajan con cada intento del ciclo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    PrimaryButton(text = if (canPlay) "¡Despertarlo!" else "Sin intentos", onClick = onStart, enabled = canPlay, loading = false)
}

@Composable
private fun ColumnScope.PlayArea(game: SalvaGame.State, onTap: () -> SalvaGame.TapResult) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                val result = onTap()
                haptics.performHapticFeedback(
                    if (result == SalvaGame.TapResult.Acierto) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
                )
            }
            .semantics { contentDescription = "Área de juego. Toca cuando el marcador esté en la zona dorada." },
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PrecisionBar(game, Modifier.fillMaxWidth().height(56.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Counter(Icons.Filled.Check, "Aciertos", game.hits, SalvaGame.HITS_TO_WIN, OroMoneda)
            Counter(Icons.Filled.Close, "Fallos", game.misses, SalvaGame.MAX_MISSES, Vida)
        }
        Text(
            when (game.lastTap) {
                SalvaGame.TapResult.Acierto -> "¡Bien! Más rápido…"
                SalvaGame.TapResult.Fallo -> "¡Casi! Espera la zona dorada."
                else -> "Toca en cualquier parte"
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrecisionBar(game: SalvaGame.State, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val marker = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.padding(horizontal = 16.dp)) {
        val barHeight = size.height * 0.45f
        val top = (size.height - barHeight) / 2
        val radius = CornerRadius(barHeight / 2)
        drawRoundRect(track, Offset(0f, top), Size(size.width, barHeight), radius)
        drawRoundRect(
            OroMoneda,
            Offset(size.width * game.zoneStart, top),
            Size(size.width * game.zoneWidth, barHeight),
            radius,
        )
        val x = size.width * game.position
        drawRoundRect(marker, Offset(x - 4.dp.toPx(), 0f), Size(8.dp.toPx(), size.height), CornerRadius(4.dp.toPx()))
    }
}


@Composable
private fun Counter(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: Int, max: Int, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text("$label: $value/$max", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Submitting(state: SalvaUiState, onRetry: () -> Unit) {
    if (state.error != null) {
        ErrorText(state.error)
        PrimaryButton(text = "Reintentar enviar resultado", onClick = onRetry, enabled = true, loading = false)
    } else {
        PrimaryButton(text = "Enviando resultado…", onClick = {}, enabled = false, loading = true)
    }
}
