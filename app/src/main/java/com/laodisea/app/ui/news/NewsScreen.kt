package com.laodisea.app.ui.news

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.model.CampusNews
import com.laodisea.app.ui.common.GameViewModel
import com.laodisea.app.ui.common.screenStatus
import com.laodisea.app.ui.templates.ListScreenTemplate
import com.laodisea.app.ui.templates.LockedState
import com.laodisea.app.ui.util.Formats

/** Noticias del campus (app.campus_news), de la más reciente a la más antigua; se leen de la caché. */
@Composable
fun NewsScreen(onBack: () -> Unit, viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val news = state.home?.news.orEmpty()

    ListScreenTemplate(
        title = "Noticias del campus",
        subtitle = if (news.isNotEmpty()) "${news.size} ${if (news.size == 1) "noticia" else "noticias"}" else null,
        status = state.screenStatus(),
        onRefresh = viewModel::refresh,
        onBack = onBack,
    ) {
        if (news.isEmpty()) {
            item {
                LockedState(
                    icon = Icons.Outlined.Newspaper,
                    title = "Aún no hay noticias",
                    message = "Cuando el campus publique avisos aparecerán aquí. Desliza hacia abajo para buscar nuevas.",
                )
            }
        }
        items(news, key = { it.id }) { NewsItem(it) }
    }
}

@Composable
private fun NewsItem(news: CampusNews) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
}
