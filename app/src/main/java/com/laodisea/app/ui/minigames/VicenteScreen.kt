package com.laodisea.app.ui.minigames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.repository.MinigameReward
import com.laodisea.app.domain.minigames.Mood
import com.laodisea.app.domain.minigames.Negotiation
import com.laodisea.app.ui.auth.ErrorText
import com.laodisea.app.ui.auth.PrimaryButton
import com.laodisea.app.ui.components.OdiseaCard
import com.laodisea.app.ui.components.StatBar
import com.laodisea.app.ui.templates.MinigameScreenTemplate
import com.laodisea.app.ui.theme.NocheAzul
import com.laodisea.app.ui.theme.OroMoneda
import com.laodisea.app.ui.theme.Resistencia
import com.laodisea.app.ui.theme.Vida
import com.laodisea.app.ui.util.GameLabels

@Composable
fun VicenteScreen(
    onBack: () -> Unit,
    onFinished: (MinigameReward) -> Unit,
    viewModel: VicenteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx by viewModel.context.collectAsStateWithLifecycle()
    LaunchedEffect(state.reward) { state.reward?.let(onFinished) }

    MinigameScreenTemplate(
        title = "Negocia con Vicente",
        onBack = onBack,
        coins = ctx.coins,
        attemptsRemaining = ctx.status?.attemptsRemaining,
        attemptsTotal = ctx.status?.attemptsPerCycle,
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val negotiation = state.negotiation
            VicenteHeader(negotiation?.mood ?: Mood.Tranquilo)
            if (negotiation == null) {
                Intro(
                    canPlay = (ctx.status?.attemptsRemaining ?: 0) > 0,
                    busy = state.busy,
                    error = state.error,
                    onStart = viewModel::start,
                )
            } else {
                val item = ctx.items[negotiation.itemCode]
                ItemOnSale(negotiation, item?.name ?: negotiation.itemCode)
                PatienceCard(negotiation)
                if (negotiation.history.isNotEmpty()) History(negotiation)
                OfferForm(
                    negotiation = negotiation,
                    coins = ctx.coins,
                    offerText = state.offerText,
                    busy = state.busy,
                    error = state.error,
                    onChange = viewModel::onOfferChange,
                    onSuggestion = viewModel::useSuggestion,
                    onSend = viewModel::sendOffer,
                )
            }
        }
    }
}

@Composable
private fun VicenteHeader(mood: Mood) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NocheAzul), contentAlignment = Alignment.Center) {
            Text("V", style = MaterialTheme.typography.headlineMedium, color = OroMoneda)
        }
        Column {
            Text("Vicente el Águila", style = MaterialTheme.typography.titleMedium)
            Text(mood.line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Intro(canPlay: Boolean, busy: Boolean, error: String?, onStart: () -> Unit) {
    OdiseaCard(title = "Cómo se juega") {
        Text(
            "Vicente te ofrece un objeto a precio inflado. Haz ofertas: si llegas a su mínimo secreto, te lo vende " +
                "por lo que ofreciste. Las ofertas muy bajas le quitan más paciencia y tienes pocos turnos.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Empezar usa un intento. Si dejaste una negociación abierta, la retomas sin gastar otro. " +
                "El objeto llega a tu mochila al abrir el juego de PC.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ErrorText(error)
    PrimaryButton(
        text = if (canPlay) "Ir con Vicente" else "Sin intentos (retomar abierta)",
        onClick = onStart,
        enabled = !busy,
        loading = busy,
    )
}

@Composable
private fun ItemOnSale(negotiation: Negotiation, name: String) {
    OdiseaCard(
        title = "En venta",
        icon = GameLabels.itemIcon(negotiation.itemCode),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.MonetizationOn, contentDescription = null)
            Text("Vicente pide ${negotiation.askingPrice} monedas", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun PatienceCard(negotiation: Negotiation) {
    val color = when (negotiation.mood) {
        Mood.Tranquilo -> Resistencia
        Mood.Impaciente -> OroMoneda
        else -> Vida
    }
    OdiseaCard {
        StatBar("Paciencia · ${negotiation.mood.label}", negotiation.patience, 100, color)
        Text(
            "Turnos restantes: ${negotiation.turnsLeft}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun History(negotiation: Negotiation) {
    OdiseaCard(title = "Regateo", icon = Icons.Outlined.Forum) {
        negotiation.history.forEach { exchange ->
            Text("Tú: ${exchange.offer} monedas", style = MaterialTheme.typography.labelLarge)
            Text(
                when (exchange.reply) {
                    Negotiation.Reply.Contraoferta -> "Vicente: «Ni loco. Te lo dejo en ${exchange.askingAfter}.»"
                    Negotiation.Reply.Aceptada -> "Vicente: «Trato hecho.»"
                    Negotiation.Reply.SeRetiro -> "Vicente: «Me voy, no tengo tiempo para esto.»"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun ColumnScope.OfferForm(
    negotiation: Negotiation,
    coins: Long,
    offerText: String,
    busy: Boolean,
    error: String?,
    onChange: (String) -> Unit,
    onSuggestion: (Long) -> Unit,
    onSend: () -> Unit,
) {
    OutlinedTextField(
        value = offerText,
        onValueChange = onChange,
        label = { Text("Tu oferta (tienes $coins monedas)") },
        singleLine = true,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        negotiation.suggestedOffers(coins).forEach { amount ->
            AssistChip(onClick = { onSuggestion(amount) }, label = { Text("$amount") }, enabled = !busy)
        }
    }
    ErrorText(error)
    PrimaryButton(text = "Ofertar", onClick = onSend, enabled = offerText.isNotEmpty() && !busy, loading = busy)
}
