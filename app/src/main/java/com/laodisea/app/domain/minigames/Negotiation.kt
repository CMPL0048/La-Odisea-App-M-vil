package com.laodisea.app.domain.minigames

import kotlin.math.roundToLong

/**
 * «Negocia con Vicente»: estado de la negociación tal como lo ve la app. El servidor guarda el
 * precio mínimo oculto y decide cada respuesta; aquí solo se valida la oferta antes de mandarla,
 * se aplica la respuesta y se calcula el ánimo de Vicente. Kotlin puro para probarlo con JUnit.
 */
data class Negotiation(
    val id: Long,
    val itemCode: String,
    val askingPrice: Long,
    val patience: Int,
    val turnsLeft: Int,
    val status: Status = Status.Abierta,
    val finalPrice: Long? = null,
    val history: List<Exchange> = emptyList(),
) {
    enum class Status { Abierta, Aceptada, SeRetiro }

    data class Exchange(val offer: Long, val reply: Reply, val askingAfter: Long?)

    enum class Reply { Contraoferta, Aceptada, SeRetiro }

    val isOpen: Boolean get() = status == Status.Abierta

    val mood: Mood get() = Mood.of(patience)

    /** Revisa la oferta antes de mandarla; null si es válida. */
    fun validateOffer(offer: Long?, coins: Long): OfferError? = when {
        !isOpen -> OfferError.Cerrada
        offer == null || offer <= 0 -> OfferError.Vacia
        offer > coins -> OfferError.SinMonedas(coins)
        offer > askingPrice -> OfferError.MayorQuePrecio(askingPrice)
        else -> null
    }

    /** Ofertas rápidas sobre el precio actual; no dependen del mínimo oculto. */
    fun suggestedOffers(coins: Long): List<Long> =
        listOf(0.4, 0.6, 0.8)
            .map { (askingPrice * it).roundToLong().coerceAtLeast(1) }
            .filter { it <= coins }
            .distinct()

    /** Aplica la respuesta del servidor a la oferta [offer]. */
    fun apply(offer: Long, response: OfferResponse): Negotiation = when (response) {
        is OfferResponse.Contraoferta -> copy(
            askingPrice = response.askingPrice,
            patience = response.patience,
            turnsLeft = response.turnsLeft,
            history = history + Exchange(offer, Reply.Contraoferta, response.askingPrice),
        )
        is OfferResponse.Aceptada -> copy(
            status = Status.Aceptada,
            finalPrice = response.finalPrice,
            history = history + Exchange(offer, Reply.Aceptada, null),
        )
        OfferResponse.SeRetiro -> copy(
            status = Status.SeRetiro,
            patience = 0,
            turnsLeft = 0,
            history = history + Exchange(offer, Reply.SeRetiro, null),
        )
    }
}

sealed interface OfferResponse {
    data class Contraoferta(val askingPrice: Long, val patience: Int, val turnsLeft: Int) : OfferResponse
    data class Aceptada(val itemCode: String, val finalPrice: Long) : OfferResponse
    data object SeRetiro : OfferResponse
}

sealed interface OfferError {
    data object Cerrada : OfferError
    data object Vacia : OfferError
    data class SinMonedas(val coins: Long) : OfferError
    data class MayorQuePrecio(val askingPrice: Long) : OfferError

    val message: String
        get() = when (this) {
            Cerrada -> "La negociación ya terminó."
            Vacia -> "Escribe cuántas monedas ofreces."
            is SinMonedas -> "No te alcanza: tienes $coins monedas."
            is MayorQuePrecio -> "Vicente pide $askingPrice; no ofrezcas más que eso."
        }
}

/** Ánimo de Vicente según su paciencia (0..100). */
enum class Mood(val label: String, val line: String) {
    Tranquilo("Tranquilo", "«Habla, jaguar. Todo tiene un precio.»"),
    Impaciente("Impaciente", "«No tengo todo el día, ¿eh?»"),
    Molesto("Molesto", "«Me estás haciendo perder el tiempo…»"),
    AlLimite("A punto de irse", "«Una más así y me largo.»");

    companion object {
        fun of(patience: Int): Mood = when {
            patience >= 75 -> Tranquilo
            patience >= 50 -> Impaciente
            patience >= 25 -> Molesto
            else -> AlLimite
        }
    }
}
