package com.laodisea.app.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laodisea.app.data.repository.SyncStatus
import com.laodisea.app.ui.components.OdiseaTopBar
import com.laodisea.app.ui.util.Formats

/*
 * Las 4 plantillas de la app (cuadrícula/lista, detalle, minijuego y formulario). Cada pantalla
 * solo arma su contenido dentro de una de ellas. La de minijuego llega en el sprint 3.
 */

/** Estado común de una pantalla que lee de la caché y se puede refrescar. */
data class ScreenStatus(
    val sync: SyncStatus,
    val hasData: Boolean,
    val loaded: Boolean,
    val fetchedAt: Long,
) {
    /** Hay aviso que mostrar (sin conexión o error con datos guardados en pantalla). */
    val showsBanner: Boolean get() = hasData && (sync.offline || sync.error != null)
}

/** Plantilla de lista: barra superior, deslizar para actualizar, aviso sin conexión y estados vacío/carga/error. */
@Composable
fun ListScreenTemplate(
    title: String,
    status: ScreenStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    coins: Long? = null,
    content: LazyListScope.() -> Unit,
) {
    ReadScaffold(title, subtitle, coins, onBack, status, onRefresh, modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (status.showsBanner) item { SyncBanner(status) }
            content()
        }
    }
}

/** Plantilla de cuadrícula: igual que la lista pero con [columns] columnas. */
@Composable
fun GridScreenTemplate(
    title: String,
    status: ScreenStatus,
    onRefresh: () -> Unit,
    columns: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    content: LazyGridScope.() -> Unit,
) {
    ReadScaffold(title, subtitle, null, onBack, status, onRefresh, modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (status.showsBanner) fullWidth { SyncBanner(status) }
            content()
        }
    }
}

/** Elemento de la cuadrícula que ocupa todo el ancho (encabezados, avisos). */
fun LazyGridScope.fullWidth(key: Any? = null, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Plantilla de detalle: encabezado grande y secciones con scroll. */
@Composable
fun DetailScreenTemplate(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { OdiseaTopBar(title = title, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = header,
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

/** Plantilla de formulario: columna con scroll para ajustes y edición. */
@Composable
fun FormScreenTemplate(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { OdiseaTopBar(title = title, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
    }
}

/** Aviso de sin conexión (datos guardados) o de error del servidor; nada si todo está bien. */
@Composable
fun SyncBanner(status: ScreenStatus) {
    val sync = status.sync
    when {
        sync.offline && status.hasData -> Surface(
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null)
                Text(
                    "Sin conexión. Datos guardados ${Formats.relativeDateTime(status.fetchedAt)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        sync.error != null && status.hasData -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(sync.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadScaffold(
    title: String,
    subtitle: String?,
    coins: Long?,
    onBack: (() -> Unit)?,
    status: ScreenStatus,
    onRefresh: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OdiseaTopBar(
                title = title,
                subtitle = subtitle,
                coins = coins,
                navigationIcon = { if (onBack != null) BackButton(onBack) },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = status.sync.refreshing && status.hasData,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                status.hasData -> content()
                !status.loaded || status.sync.refreshing -> CenteredLoading()
                else -> EmptyState(
                    message = status.sync.error ?: "Aún no hay datos guardados. Conéctate para descargarlos.",
                    onRetry = onRefresh,
                )
            }
        }
    }
}

@Composable
fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun EmptyState(message: String, onRetry: (() -> Unit)? = null) {
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
        if (onRetry != null) Button(onClick = onRetry) { Text("Reintentar") }
    }
}

/** Mensaje centrado para una sección bloqueada (por ejemplo, sin el objeto Mapa). */
@Composable
fun LockedState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Plantilla de minijuego: barra con regreso y monedas, franja con los intentos que quedan y el
 * área de juego, que ocupa el resto de la pantalla.
 */
@Composable
fun MinigameScreenTemplate(
    title: String,
    onBack: () -> Unit,
    coins: Long?,
    attemptsRemaining: Int?,
    attemptsTotal: Int?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { OdiseaTopBar(title = title, coins = coins, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (attemptsRemaining != null && attemptsTotal != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Intentos", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        com.laodisea.app.ui.components.AttemptDots(attemptsRemaining, attemptsTotal)
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}
