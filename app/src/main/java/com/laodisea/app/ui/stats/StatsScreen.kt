package com.laodisea.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.domain.StatsSummary
import com.laodisea.app.domain.statsSummary
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.common.screenStatus
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.components.StatBar
import com.laodisea.app.ui.templates.ListScreenTemplate
import com.laodisea.app.ui.theme.Experiencia
import com.laodisea.app.ui.theme.Resistencia
import com.laodisea.app.ui.theme.Vida
import com.laodisea.app.ui.util.Formats

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    ListScreenTemplate(
        title = "Estadísticas",
        status = state.screenStatus(),
        onRefresh = viewModel::refresh,
        onBack = onBack,
        coins = snapshot?.coins,
    ) {
        if (snapshot != null) {
            val summary = statsSummary(snapshot, state.catalog)
            item { CombatCard(summary) }
            item { ProgressCard(summary) }
            item { CollectionCard(summary) }
        }
    }
}

@Composable
private fun CombatCard(s: StatsSummary) {
    OdiseaCard(title = "Víctor", icon = Icons.Outlined.Star) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            BigNumber("Nivel", s.level.toString())
            BigNumber("XP", s.xp.toString())
            BigNumber("Monedas", s.coins.toString())
        }
        StatBar("Vida", s.health, s.healthMax, Vida, icon = Icons.Outlined.Favorite)
        StatBar("Resistencia", s.resistance, s.resistanceMax, Resistencia, icon = Icons.Outlined.Shield)
    }
}

@Composable
private fun ProgressCard(s: StatsSummary) {
    OdiseaCard(title = "Avance", icon = Icons.Outlined.Explore) {
        StatBar("Subniveles completados", s.sublevelsCompleted, s.sublevelsTotal, Experiencia)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StatRow(Icons.Outlined.Explore, "Campus completados", "${s.zonesCompleted} de ${s.zonesTotal}")
        StatRow(Icons.Outlined.EmojiEvents, "Enemigos derrotados", s.enemiesDefeated.toString())
        StatRow(Icons.Outlined.Timer, "Tiempo de juego", Formats.playtime(s.playtimeSeconds))
    }
}

@Composable
private fun CollectionCard(s: StatsSummary) {
    OdiseaCard(title = "Colección", icon = Icons.Outlined.Backpack) {
        StatRow(Icons.Outlined.Backpack, "Objetos en la mochila", "${s.itemsCarried} de ${s.capacity}")
        StatRow(Icons.AutoMirrored.Outlined.MenuBook, "Personajes descubiertos", "${s.charactersUnlocked} de ${s.charactersTotal}")
    }
}

@Composable
private fun BigNumber(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
