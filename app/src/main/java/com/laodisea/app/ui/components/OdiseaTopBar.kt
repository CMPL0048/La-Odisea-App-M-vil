package com.laodisea.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.laodisea.app.ui.theme.BosqueProfundo
import com.laodisea.app.ui.theme.Papel

/** Barra superior verde con avatar opcional, título y contador de monedas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdiseaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    avatarUrl: String? = null,
    showAvatar: Boolean = false,
    coins: Long? = null,
    navigationIcon: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = navigationIcon,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showAvatar) Avatar(name = title, url = avatarUrl)
                androidx.compose.foundation.layout.Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        },
        actions = { if (coins != null) CoinChip(coins, Modifier.padding(end = 12.dp)) },
        // Verde de marca en ambos temas, para que los íconos de la barra de estado siempre sean claros.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BosqueProfundo,
            titleContentColor = Papel,
            navigationIconContentColor = Papel,
            actionIconContentColor = Papel,
        ),
    )
}

@Composable
fun CoinChip(coins: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$coins monedas" },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(coins.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun Avatar(name: String, url: String?, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier.size(size.dp).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Surface(color = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary) {
            Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp),
            )
        }
    }
}
