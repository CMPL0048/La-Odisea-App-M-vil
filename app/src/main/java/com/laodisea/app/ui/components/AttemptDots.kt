package com.laodisea.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Intentos restantes de un minijuego como puntos dorados («3/4»). */
@Composable
fun AttemptDots(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier,
    emptyColor: Color = MaterialTheme.colorScheme.outlineVariant,
    textColor: Color = Color.Unspecified,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics { contentDescription = "$remaining de $total intentos" },
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (i < remaining) MaterialTheme.colorScheme.tertiary else emptyColor),
            )
        }
        Text(
            "$remaining/$total",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
