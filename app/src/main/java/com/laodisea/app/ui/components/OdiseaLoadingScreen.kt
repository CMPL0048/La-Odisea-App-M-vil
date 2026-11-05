package com.laodisea.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laodisea.app.R
import com.laodisea.app.ui.theme.BosqueClaro
import com.laodisea.app.ui.theme.BosqueMedio
import com.laodisea.app.ui.theme.BosqueOscuro
import com.laodisea.app.ui.theme.BosqueProfundo
import com.laodisea.app.ui.theme.NocheAzul
import com.laodisea.app.ui.theme.NocheClara
import com.laodisea.app.ui.theme.NocheProfunda
import com.laodisea.app.ui.theme.OroClaro
import com.laodisea.app.ui.theme.OroMoneda
import com.laodisea.app.ui.theme.Papel

/**
 * Variantes cromáticas de la pantalla de carga. Todas parten de la paleta oficial de La Odisea
 * ([com.laodisea.app.ui.theme]) para mantener coherencia visual cambiando el ambiente según el escenario.
 */
enum class LoadingPalette(
    val topColor: Color,
    val bottomColor: Color,
    val haloColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val indicatorColor: Color,
) {
    /** Apertura inicial de la app (verde identidad de La Odisea). */
    Bosque(
        topColor = BosqueProfundo,
        bottomColor = BosqueOscuro,
        haloColor = BosqueClaro.copy(alpha = 0.09f),
        titleColor = Papel,
        subtitleColor = BosqueClaro,
        indicatorColor = OroMoneda,
    ),

    /** Inicio de sesión, sincronización de cuenta y cambio de partida activa. */
    Travesia(
        topColor = NocheAzul,
        bottomColor = NocheProfunda,
        haloColor = OroMoneda.copy(alpha = 0.11f),
        titleColor = Papel,
        subtitleColor = NocheClara,
        indicatorColor = OroMoneda,
    ),

    /** Cierre de sesión y regreso a la portada de acceso. */
    Despedida(
        topColor = BosqueMedio,
        bottomColor = NocheAzul,
        haloColor = Papel.copy(alpha = 0.09f),
        titleColor = Papel,
        subtitleColor = BosqueClaro,
        indicatorColor = OroClaro,
    ),

    /** Entrada a los minijuegos (Despierta a Salva y Negocia con Vicente). */
    Minijuego(
        topColor = NocheProfunda,
        bottomColor = BosqueMedio,
        haloColor = OroMoneda.copy(alpha = 0.15f),
        titleColor = OroClaro,
        subtitleColor = Papel,
        indicatorColor = OroMoneda,
    ),

    /** Carga interna de secciones cuando aún no existen datos en caché. */
    Seccion(
        topColor = NocheAzul,
        bottomColor = BosqueProfundo,
        haloColor = NocheClara.copy(alpha = 0.10f),
        titleColor = Papel,
        subtitleColor = NocheClara,
        indicatorColor = OroMoneda,
    ),
}

/**
 * Pantalla de carga con identidad de La Odisea. Usa [compact] cuando vive dentro de un `Scaffold`
 * que ya muestra la barra superior o inferior.
 */
@Composable
fun OdiseaLoadingScreen(
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    palette: LoadingPalette = LoadingPalette.Travesia,
    compact: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "odiseaLoading")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloPulse",
    )
    val logoSize = if (compact) 136.dp else 188.dp
    val haloSize = if (compact) 168.dp else 228.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.topColor, palette.bottomColor)))
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = if (subtitle != null) "$message. $subtitle" else message
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(haloSize)
                        .scale(pulse)
                        .background(palette.haloColor, CircleShape),
                )
                Image(
                    painter = painterResource(R.drawable.logo_odisea),
                    contentDescription = null,
                    modifier = Modifier.size(logoSize),
                )
            }
            Text(
                text = message,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = palette.titleColor,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.subtitleColor,
                    textAlign = TextAlign.Center,
                )
            }
            CircularProgressIndicator(
                color = palette.indicatorColor,
                trackColor = palette.titleColor.copy(alpha = 0.16f),
                strokeWidth = 3.dp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(28.dp),
            )
        }
    }
}

/** Superposición animada de carga que bloquea toques mientras termina una acción (ej. cambiar partida). */
@Composable
fun OdiseaLoadingOverlay(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    palette: LoadingPalette = LoadingPalette.Travesia,
    compact: Boolean = false,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(220)),
        modifier = modifier,
    ) {
        OdiseaLoadingScreen(
            message = message,
            subtitle = subtitle,
            palette = palette,
            compact = compact,
            modifier = Modifier.pointerInput(Unit) { detectTapGestures { } },
        )
    }
}
