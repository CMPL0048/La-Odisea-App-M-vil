package com.laodisea.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.model.CampusNews
import com.laodisea.app.data.model.HomeData
import com.laodisea.app.data.model.MinigameStatus
import com.laodisea.app.data.model.PlayerSnapshot
import com.laodisea.app.ui.components.AttemptDots
import com.laodisea.app.ui.components.LoadingPalette
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.components.OdiseaLoadingOverlay
import com.laodisea.app.ui.components.OdiseaLoadingScreen
import com.laodisea.app.ui.components.OdiseaTopBar
import com.laodisea.app.ui.components.StatBar
import com.laodisea.app.ui.profile.SavePickerSheet
import com.laodisea.app.ui.profile.SavesViewModel
import com.laodisea.app.ui.templates.ScreenStatus
import com.laodisea.app.ui.templates.SyncBanner
import com.laodisea.app.ui.theme.BosqueProfundo
import com.laodisea.app.ui.theme.Papel
import com.laodisea.app.ui.theme.Resistencia
import com.laodisea.app.ui.theme.Vida
import com.laodisea.app.ui.util.Formats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenStats: () -> Unit,
    onOpenMinigames: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    savesViewModel: SavesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saves by savesViewModel.list.collectAsStateWithLifecycle()
    val savesState by savesViewModel.state.collectAsStateWithLifecycle()
    var showSaves by rememberSaveable { mutableStateOf(false) }
    var lastSaveMessage by remember { mutableStateOf("Cambiando de partida…") }
    savesState.loadingMessage?.let { lastSaveMessage = it }

    val snapshot = state.data?.snapshot
    val name = state.data?.profile?.displayName ?: snapshot?.player?.displayName ?: "Jugador"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OdiseaTopBar(
                title = name,
                subtitle = snapshot?.stats?.let { "Nivel ${it.level}" },
                avatarUrl = state.data?.profile?.avatarUrl,
                showAvatar = true,
                onAvatarClick = onOpenProfile,
                coins = snapshot?.coins,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshBox(
                isRefreshing = state.refreshing && state.data != null,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val data = state.data
                when {
                    data != null -> HomeContent(data, state, onOpenStats, onOpenMinigames, onOpenNews, onOpenSaves = { showSaves = true })
                    state.refreshing -> OdiseaLoadingScreen(
                        message = "Sincronizando con el campus…",
                        subtitle = "Descargando el estado de tu partida",
                        palette = LoadingPalette.Seccion,
                        compact = true,
                    )
                    else -> HomeError(message = state.error ?: "No se pudo cargar tu partida.", onRetry = viewModel::refresh)
                }
            }
            OdiseaLoadingOverlay(
                visible = savesState.loadingMessage != null,
                message = lastSaveMessage,
                subtitle = "Sincronizando progreso y recompensas",
                palette = LoadingPalette.Travesia,
                compact = true,
            )
        }
    }
    if (showSaves) {
        SavePickerSheet(
            saves = saves,
            busy = savesState.busy,
            onSelect = { save ->
                showSaves = false
                savesViewModel.activate(save)
            },
            onDismiss = { showSaves = false },
        )
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    state: HomeUiState,
    onOpenStats: () -> Unit,
    onOpenMinigames: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenSaves: () -> Unit,
) {
    val snapshot = data.snapshot
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val status = ScreenStatus(state.sync, hasData = true, loaded = true, fetchedAt = data.fetchedAt)
        if (status.showsBanner) item { SyncBanner(status) }
        item { StatsCard(snapshot, onOpenStats) }
        item { ZoneCard(snapshot, data.activeSaveName, onOpenSaves) }
        item { JuanmaCard(JuanmaTips.forHome(data)) }
        item { MinigamesCard(data.minigames, onOpenMinigames) }
        item { NewsCard(data.latestNews, onOpenNews) }
    }
}

@Composable
private fun StatsCard(snapshot: PlayerSnapshot, onOpenStats: () -> Unit) {
    val stats = snapshot.stats ?: return
    OdiseaCard(
        title = "Tus stats",
        icon = Icons.Outlined.Star,
        trailing = { TextButton(onClick = onOpenStats) { Text("Ver todo") } },
    ) {
        StatBar("Vida", stats.health, stats.healthMax, Vida, icon = Icons.Outlined.Favorite)
        StatBar("Resistencia", stats.resistance, stats.resistanceMax, Resistencia, icon = Icons.Outlined.Shield)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat("Nivel", stats.level.toString())
            MiniStat("XP", stats.xp.toString())
            MiniStat("Enemigos", stats.enemiesDefeated.toString())
            MiniStat("Jugado", Formats.playtime(stats.playtimeSeconds))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ZoneCard(snapshot: PlayerSnapshot, saveName: String?, onOpenSaves: () -> Unit) {
    val zone = snapshot.currentZone
    val progress = snapshot.progress
    OdiseaCard(
        title = "Zona actual",
        icon = Icons.Outlined.Explore,
        containerColor = BosqueProfundo,
        contentColor = Papel,
        // Sin la migración de partidas en el servidor no se conoce el nombre y no hay chip.
        trailing = saveName?.let { name -> { SaveChip(name, onOpenSaves) } },
    ) {
        Text(zone?.name ?: "Sin zona", style = MaterialTheme.typography.headlineSmall)
        if (zone != null && zone.sublevelsTotal > 0) {
            Text(
                "Subnivel ${zone.sublevelsCompleted} de ${zone.sublevelsTotal}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Progreso de la odisea", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("${(progress * 100).toInt()} %", style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(MaterialTheme.shapes.small)
                    .semantics { contentDescription = "Progreso total ${(progress * 100).toInt()} por ciento" },
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = Papel.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                "${snapshot.completedZones} de ${snapshot.zones.size} campus completados",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** Partida activa; tocarla abre la hoja para cambiar de partida. */
@Composable
private fun SaveChip(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        modifier = Modifier.semantics { contentDescription = "Partida activa: $name. Toca para cambiar de partida" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun JuanmaCard(tip: String) {
    OdiseaCard(
        title = "Consejo de Juanma",
        icon = Icons.Outlined.Lightbulb,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Text("J", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onTertiary)
            }
            Text(
                "“$tip”",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun MinigamesCard(minigames: List<MinigameStatus>, onOpenMinigames: () -> Unit) {
    OdiseaCard(
        title = "Minijuegos",
        icon = Icons.Outlined.SportsEsports,
        trailing = { TextButton(onClick = onOpenMinigames) { Text("Jugar") } },
    ) {
        if (minigames.isEmpty()) {
            Text("No hay minijuegos disponibles.", style = MaterialTheme.typography.bodyMedium)
        }
        minigames.forEachIndexed { index, game ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MinigameRow(game)
        }
    }
}

@Composable
private fun MinigameRow(game: MinigameStatus) {
    val full = game.attemptsRemaining >= game.attemptsPerCycle
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(game.name, style = MaterialTheme.typography.titleSmall)
            Text(
                if (full) "Intentos completos" else "Se recargan ${Formats.untilReset(game.nextResetAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AttemptDots(game.attemptsRemaining, game.attemptsPerCycle)
    }
}

@Composable
private fun NewsCard(news: CampusNews?, onOpenNews: () -> Unit) {
    OdiseaCard(
        title = "Última noticia",
        icon = Icons.Outlined.Newspaper,
        trailing = { TextButton(onClick = onOpenNews) { Text("Ver todas") } },
    ) {
        if (news == null) {
            Text(
                "Aún no hay noticias del campus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@OdiseaCard
        }
        news.category?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(news.title, style = MaterialTheme.typography.titleMedium)
        news.summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        news.publishedAt?.let {
            Text(Formats.date(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
