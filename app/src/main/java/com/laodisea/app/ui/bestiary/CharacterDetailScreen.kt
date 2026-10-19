package com.laodisea.app.ui.bestiary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.domain.bestiary
import com.laodisea.app.ui.components.OdiseaChip
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.templates.CenteredLoading
import com.laodisea.app.ui.templates.DetailScreenTemplate
import com.laodisea.app.ui.templates.EmptyState
import com.laodisea.app.ui.util.GameLabels

@Composable
fun CharacterDetailScreen(code: String, onBack: () -> Unit, viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot
    // Solo se muestra la ficha de personajes desbloqueados.
    val entry = snapshot?.let { bestiary(it, state.catalog) }?.firstOrNull { it.character.code == code && it.unlocked }

    if (entry == null) {
        DetailScreenTemplate(title = "Personaje", onBack = onBack, header = {}) {
            if (state.loaded) EmptyState("Aún no has descubierto a este personaje.") else CenteredLoading()
        }
        return
    }
    val character = entry.character
    val zones = state.catalog?.zones.orEmpty().associateBy { it.code }

    DetailScreenTemplate(
        title = character.name,
        onBack = onBack,
        header = {
            CharacterPortrait(entry, size = 120)
            Text(character.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OdiseaChip(GameLabels.role(character.role))
                character.university?.let { OdiseaChip(it) }
            }
        },
    ) {
        character.quote?.let {
            OdiseaCard(title = "Frase", icon = Icons.Outlined.FormatQuote) {
                Text("“$it”", style = MaterialTheme.typography.bodyLarge)
            }
        }
        OdiseaCard(title = "Personalidad", icon = Icons.Outlined.Psychology) {
            Text(character.personalityTraits ?: "Pronto sabremos más de este personaje.", style = MaterialTheme.typography.bodyLarge)
        }
        character.firstEncounterZone?.let { zoneCode ->
            OdiseaCard(title = "Primer encuentro", icon = Icons.Outlined.School) {
                Text(zones[zoneCode]?.name ?: zoneCode, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
