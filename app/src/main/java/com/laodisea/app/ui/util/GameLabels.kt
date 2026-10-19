package com.laodisea.app.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AirportShuttle
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Map
import androidx.compose.ui.graphics.vector.ImageVector

/** Textos e íconos de los códigos del catálogo mientras llega el arte final. */
object GameLabels {

    fun category(code: String?): String = when (code) {
        "clave" -> "Objeto clave"
        "equipo" -> "Equipo"
        "curacion" -> "Curación"
        null -> "Otros"
        else -> code.replaceFirstChar { it.uppercase() }
    }

    fun role(code: String?): String = when (code) {
        "protagonista" -> "Protagonista"
        "aliado" -> "Aliado"
        "jefe" -> "Jefe"
        null -> "Personaje"
        else -> code.replaceFirstChar { it.uppercase() }
    }

    fun rarity(code: String?): String? = code?.replaceFirstChar { it.uppercase() }

    fun itemIcon(code: String): ImageVector = when (code) {
        "MAPA" -> Icons.Outlined.Map
        "COMBI" -> Icons.Outlined.AirportShuttle
        "ESPATULA" -> Icons.Outlined.Healing
        "CHALECO" -> Icons.Outlined.Checkroom
        "MUESTRARIO" -> Icons.Outlined.Category
        "MOCHILA" -> Icons.Outlined.Backpack
        else -> Icons.Outlined.Inventory2
    }
}
