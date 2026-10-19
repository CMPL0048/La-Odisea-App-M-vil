package com.laodisea.app.ui.backpack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.domain.backpackSlots
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.common.screenStatus
import com.laodisea.app.ui.components.InventorySlot
import com.laodisea.app.ui.templates.GridScreenTemplate
import com.laodisea.app.ui.templates.fullWidth
import com.laodisea.app.ui.util.GameLabels

@Composable
fun BackpackScreen(
    onOpenItem: (String) -> Unit,
    onOpenShowcase: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot
    val slots = snapshot?.let(::backpackSlots).orEmpty()
    val used = slots.count { it != null }

    GridScreenTemplate(
        title = "Mochila",
        subtitle = snapshot?.let { "$used de ${it.capacity} espacios" },
        status = state.screenStatus(),
        onRefresh = viewModel::refresh,
        columns = 4,
    ) {
        if (snapshot == null) return@GridScreenTemplate
        fullWidth("capacity") { CapacityBar(used, snapshot.capacity) }
        itemsIndexed(slots, key = { index, item -> item?.code ?: "empty-$index" }) { _, item ->
            InventorySlot(
                name = item?.name,
                quantity = item?.quantity ?: 0,
                icon = GameLabels.itemIcon(item?.code.orEmpty()),
                onClick = item?.let { { onOpenItem(it.code) } },
            )
        }
        fullWidth("showcase") { ShowcaseLink(onOpenShowcase) }
    }
}

@Composable
private fun CapacityBar(used: Int, capacity: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { if (capacity > 0) used.toFloat() / capacity else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Text(
            "Toca un objeto para ver su detalle.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShowcaseLink(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Category, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text("Muestrario", style = MaterialTheme.typography.titleSmall)
                Text("Todos los objetos del juego", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
