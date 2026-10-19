package com.laodisea.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = BosqueProfundo,
    onPrimary = Papel,
    primaryContainer = BosqueClaro,
    onPrimaryContainer = BosqueOscuro,
    secondary = NocheAzul,
    onSecondary = Papel,
    secondaryContainer = NocheClara,
    onSecondaryContainer = NocheProfunda,
    tertiary = OroMoneda,
    onTertiary = Tinta,
    tertiaryContainer = OroClaro,
    onTertiaryContainer = OroOscuro,
    background = Papel,
    onBackground = Tinta,
    surface = Papel,
    onSurface = Tinta,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE3E9E6),
    onSurfaceVariant = TintaSuave,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFFBFCFB),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEDF1EF),
    outline = Borde,
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFDCE2DF),
    error = Error,
    errorContainer = ErrorClaro,
)

private val DarkColors = darkColorScheme(
    primary = OroMoneda,
    onPrimary = BosqueOscuro,
    primaryContainer = BosqueMedio,
    onPrimaryContainer = Papel,
    secondary = NocheClara,
    onSecondary = NocheProfunda,
    secondaryContainer = NocheAzul,
    onSecondaryContainer = Papel,
    tertiary = OroMoneda,
    onTertiary = Tinta,
    tertiaryContainer = OroOscuro,
    onTertiaryContainer = OroClaro,
    background = BosqueOscuro,
    onBackground = Papel,
    surface = BosqueOscuro,
    onSurface = Papel,
    surfaceVariant = BosqueProfundo,
    onSurfaceVariant = BosqueClaro,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF041A11),
    surfaceContainerLow = BosqueProfundo,
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF12452D),
    outline = androidx.compose.ui.graphics.Color(0xFF5E7A6B),
    outlineVariant = BosqueMedio,
)

val OdiseaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun LaOdiseaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = OdiseaTypography,
        shapes = OdiseaShapes,
        content = content,
    )
}
