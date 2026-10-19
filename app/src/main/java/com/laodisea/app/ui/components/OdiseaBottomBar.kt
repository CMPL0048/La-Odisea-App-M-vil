package com.laodisea.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Las 5 secciones principales de la app. */
enum class MainSection(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Inicio("Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    Mapa("Mapa", Icons.Outlined.Map, Icons.Filled.Map),
    Mochila("Mochila", Icons.Outlined.Backpack, Icons.Filled.Backpack),
    Bestiario("Bestiario", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Mas("Más", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz),
}

@Composable
fun OdiseaBottomBar(
    selected: MainSection,
    onSelect: (MainSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        MainSection.entries.forEach { section ->
            val isSelected = section == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(section) },
                icon = { Icon(if (isSelected) section.selectedIcon else section.icon, contentDescription = null) },
                label = { Text(section.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onTertiary,
                    indicatorColor = MaterialTheme.colorScheme.tertiary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}
