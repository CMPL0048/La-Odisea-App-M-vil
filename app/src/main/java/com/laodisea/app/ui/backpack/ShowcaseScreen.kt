package com.laodisea.app.ui.backpack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.model.ItemCodes
import com.laodisea.app.domain.ShowcaseEntry
import com.laodisea.app.domain.hasItem
import com.laodisea.app.domain.showcase
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.common.screenStatus
import com.laodisea.app.ui.templates.ListScreenTemplate
import com.laodisea.app.ui.util.GameLabels

/** Catálogo de todos los objetos. Con el Muestrario de materiales se ve la información de todos. */
@Composable
fun ShowcaseScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    ListScreenTemplate(
        title = "Muestrario",
        status = state.screenStatus(),
        onRefresh = viewModel::refresh,
        onBack = onBack,
    ) {
        if (snapshot == null) return@ListScreenTemplate
        if (!snapshot.hasItem(ItemCodes.MUESTRARIO)) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Category, contentDescription = null)
                        Text(
                            "Consigue el Muestrario de materiales para ver la información de los objetos que aún no tienes.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
            }
        }
        showcase(snapshot, state.catalog).forEach { section ->
            item(key = "header-${section.category}") {
                Text(
                    GameLabels.category(section.category),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(section.entries, key = { it.item.code }) { entry ->
                ShowcaseRow(entry, onClick = if (entry.revealed) ({ onOpenItem(entry.item.code) }) else null)
            }
        }
    }
}

@Composable
private fun ShowcaseRow(entry: ShowcaseEntry, onClick: (() -> Unit)?) {
    val name = if (entry.revealed) entry.item.name else "???"
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = if (entry.owned) "$name, en tu mochila" else "$name, no lo tienes"
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                GameLabels.itemIcon(entry.item.code),
                contentDescription = null,
                tint = if (entry.revealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (entry.revealed) entry.item.description.orEmpty() else "Objeto no descubierto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Icon(
                if (entry.owned) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = if (entry.owned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
