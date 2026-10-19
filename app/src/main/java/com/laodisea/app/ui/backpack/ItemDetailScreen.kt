package com.laodisea.app.ui.backpack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.laodisea.app.domain.itemDetail
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.components.OdiseaChip
import com.laodisea.app.ui.templates.CenteredLoading
import com.laodisea.app.ui.templates.DetailScreenTemplate
import com.laodisea.app.ui.templates.EmptyState
import com.laodisea.app.ui.util.GameLabels

@Composable
fun ItemDetailScreen(code: String, onBack: () -> Unit, viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = itemDetail(code, state.snapshot, state.catalog)

    if (item == null) {
        DetailScreenTemplate(title = "Objeto", onBack = onBack, header = {}) {
            if (state.loaded) EmptyState("No encontramos este objeto.") else CenteredLoading()
        }
        return
    }

    DetailScreenTemplate(
        title = item.name,
        onBack = onBack,
        header = {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (item.iconUrl != null) {
                    AsyncImage(model = item.iconUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(64.dp))
                } else {
                    Icon(
                        GameLabels.itemIcon(item.code),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(item.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OdiseaChip(GameLabels.category(item.category))
                GameLabels.rarity(item.rarity)?.let { OdiseaChip(it) }
                OdiseaChip(if (item.quantity > 0) "Tienes ${item.quantity}" else "No lo tienes")
            }
        },
    ) {
        OdiseaCard(title = "Descripción", icon = Icons.Outlined.Description) {
            Text(item.description ?: "Sin descripción por ahora.", style = MaterialTheme.typography.bodyLarge)
        }
        OdiseaCard(title = "Efecto", icon = Icons.Outlined.AutoAwesome) {
            Text(item.effect ?: "Sin efecto especial.", style = MaterialTheme.typography.bodyLarge)
        }
        item.price?.let {
            Text(
                "Precio de referencia con Vicente: $it monedas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
