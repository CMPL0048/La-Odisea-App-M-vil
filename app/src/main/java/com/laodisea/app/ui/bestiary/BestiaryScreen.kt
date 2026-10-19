package com.laodisea.app.ui.bestiary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.laodisea.app.domain.BestiaryEntry
import com.laodisea.app.domain.bestiary
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.common.screenStatus
import com.laodisea.app.ui.templates.GridScreenTemplate
import com.laodisea.app.ui.util.GameLabels

@Composable
fun BestiaryScreen(onOpenCharacter: (String) -> Unit, viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot
    val entries = snapshot?.let { bestiary(it, state.catalog) }.orEmpty()

    GridScreenTemplate(
        title = "Bestiario",
        subtitle = snapshot?.let { "${entries.count { it.unlocked }} de ${entries.size} personajes descubiertos" },
        status = state.screenStatus(),
        onRefresh = viewModel::refresh,
        columns = 3,
    ) {
        items(entries, key = { it.character.code }) { entry ->
            CharacterTile(entry, onClick = if (entry.unlocked) ({ onOpenCharacter(entry.character.code) }) else null)
        }
    }
}

@Composable
private fun CharacterTile(entry: BestiaryEntry, onClick: (() -> Unit)?) {
    val character = entry.character
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = MaterialTheme.shapes.large,
        color = if (entry.unlocked) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = if (entry.unlocked) "${character.name}, ${GameLabels.role(character.role)}" else "Personaje sin descubrir"
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CharacterPortrait(entry, size = 64)
            Text(
                if (entry.unlocked) character.name else "???",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (entry.unlocked) GameLabels.role(character.role) else "Sin descubrir",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ilustración del personaje o, mientras llega el arte, su inicial; bloqueado se ve con candado. */
@Composable
fun CharacterPortrait(entry: BestiaryEntry, size: Int) {
    val character = entry.character
    val bg = when {
        !entry.unlocked -> MaterialTheme.colorScheme.outlineVariant
        character.role == "jefe" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(Modifier.size(size.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        when {
            !entry.unlocked -> Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((size / 2.5).dp),
            )
            character.illustrationUrl != null -> AsyncImage(
                model = character.illustrationUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp),
            )
            else -> Text(
                character.name.take(1),
                style = if (size > 80) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
