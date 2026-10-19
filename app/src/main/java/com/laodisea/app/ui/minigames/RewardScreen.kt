package com.laodisea.app.ui.minigames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.repository.MinigameRepository
import com.laodisea.app.ui.auth.PrimaryButton
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.templates.MinigameScreenTemplate
import com.laodisea.app.ui.util.GameLabels

/** Resultado de una partida: monedas ganadas o gastadas y objeto que espera en el juego de PC. */
@Composable
fun RewardScreen(
    code: String,
    success: Boolean,
    coinsDelta: Long,
    itemCode: String?,
    onPlayAgain: () -> Unit,
    onDone: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val game = state.home?.minigames?.firstOrNull { it.code == code }
    val itemName = itemCode?.let { c -> state.catalog?.items?.firstOrNull { it.code == c }?.name ?: c }
    val isVicente = code == MinigameRepository.CODE_VICENTE

    MinigameScreenTemplate(
        title = "Recompensa",
        onBack = onDone,
        coins = state.snapshot?.coins,
        attemptsRemaining = game?.attemptsRemaining,
        attemptsTotal = game?.attemptsPerCycle,
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (success) Icons.Filled.EmojiEvents else Icons.Outlined.SentimentDissatisfied,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (success) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                when {
                    success && isVicente -> "¡Trato hecho!"
                    success -> "¡Salva despertó!"
                    isVicente -> "Vicente se fue"
                    else -> "Salva sigue dormido"
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            OdiseaCard {
                if (coinsDelta != 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (coinsDelta > 0) "+$coinsDelta monedas" else "$coinsDelta monedas",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                if (itemName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(GameLabels.itemIcon(itemCode), contentDescription = null)
                        Text(itemName, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Llegará a tu mochila la próxima vez que abras el juego en la PC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (coinsDelta == 0L && itemName == null) {
                    Text(
                        if (isVicente) "No gastaste monedas. Intenta ofertas menos agresivas." else "Esta vez no hubo monedas. ¡Inténtalo de nuevo!",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        val canReplay = (game?.attemptsRemaining ?: 0) > 0
        if (canReplay) PrimaryButton(text = "Jugar otra vez", onClick = onPlayAgain, enabled = true, loading = false)
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.SportsEsports, contentDescription = null)
            Text("Volver a minijuegos", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
