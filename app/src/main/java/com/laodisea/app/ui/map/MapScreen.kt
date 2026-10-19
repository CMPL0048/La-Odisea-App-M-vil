package com.laodisea.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AirportShuttle
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.domain.MapZone
import com.laodisea.app.domain.MapZoneState
import com.laodisea.app.domain.canSeeMap
import com.laodisea.app.domain.mapZones
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.common.screenStatus
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.templates.ListScreenTemplate
import com.laodisea.app.ui.templates.LockedState
import com.laodisea.app.ui.theme.BosqueProfundo
import com.laodisea.app.ui.theme.OroMoneda

@Composable
fun MapScreen(viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    ListScreenTemplate(
        title = "Mapa",
        subtitle = snapshot?.let { "${it.completedZones} de ${it.zones.size} campus completados" },
        status = state.screenStatus(),
        onRefresh = viewModel::refresh,
    ) {
        if (snapshot == null) return@ListScreenTemplate
        if (!snapshot.canSeeMap()) {
            item {
                LockedState(
                    icon = Icons.Outlined.Map,
                    title = "Aún no tienes el mapa",
                    message = "Consigue el objeto Mapa en el juego para ver los campus que has recorrido y los que faltan.",
                )
            }
            return@ListScreenTemplate
        }
        val zones = mapZones(snapshot, state.catalog)
        itemsIndexed(zones, key = { _, zone -> zone.code }) { index, zone ->
            ZoneStop(zone, isFirst = index == 0, isLast = index == zones.lastIndex)
        }
    }
}

/** Una parada del recorrido: línea vertical con el estado y la tarjeta de la zona. */
@Composable
private fun ZoneStop(zone: MapZone, isFirst: Boolean, isLast: Boolean) {
    val done = zone.state == MapZoneState.Completada
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            modifier = Modifier.width(44.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(3.dp).height(18.dp).background(if (isFirst) Color.Transparent else lineColor))
            StopMarker(zone.state)
            Box(Modifier.width(3.dp).weight(1f).background(if (isLast) Color.Transparent else if (done) OroMoneda else lineColor))
        }
        ZoneCard(zone, Modifier.padding(start = 8.dp, bottom = if (isLast) 0.dp else 4.dp))
    }
}

@Composable
private fun StopMarker(state: MapZoneState) {
    val (bg, fg, icon) = when (state) {
        MapZoneState.Completada -> Triple(OroMoneda, BosqueProfundo, Icons.Filled.Check)
        MapZoneState.Actual -> Triple(BosqueProfundo, OroMoneda, Icons.Filled.Star)
        MapZoneState.Bloqueada -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.Lock)
        MapZoneState.Oculta -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.QuestionMark)
    }
    Box(Modifier.size(36.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ZoneCard(zone: MapZone, modifier: Modifier = Modifier) {
    val current = zone.state == MapZoneState.Actual
    val dimmed = zone.state == MapZoneState.Bloqueada || zone.state == MapZoneState.Oculta
    OdiseaCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "${zone.name}, ${stateLabel(zone.state)}"
        },
        containerColor = if (current) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(stateLabel(zone.state).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(zone.name, style = MaterialTheme.typography.titleMedium)
        if (zone.state != MapZoneState.Oculta) {
            if (zone.sublevelsTotal > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Subniveles: ${zone.sublevelsCompleted} de ${zone.sublevelsTotal}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = { zone.sublevelsCompleted.toFloat() / zone.sublevelsTotal },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                        color = if (done(zone)) OroMoneda else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            } else {
                Text("Zona de paso, sin subniveles.", style = MaterialTheme.typography.bodySmall)
            }
            zone.bossName?.let {
                Text(
                    if (dimmed) "Jefe: ???" else "Jefe: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (zone.routeUnlocked) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AirportShuttle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Ruta en combi desbloqueada", style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            Text("Llega hasta aquí para descubrir este campus.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun done(zone: MapZone) = zone.state == MapZoneState.Completada

private fun stateLabel(state: MapZoneState) = when (state) {
    MapZoneState.Completada -> "Completado"
    MapZoneState.Actual -> "Estás aquí"
    MapZoneState.Bloqueada -> "Bloqueado"
    MapZoneState.Oculta -> "Oculto"
}
