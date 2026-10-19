package com.laodisea.app.data.repository

import com.laodisea.app.domain.minigames.Negotiation
import com.laodisea.app.domain.minigames.OfferResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de un intento de minijuego; el servidor fija las monedas y el objeto. */
data class MinigameReward(
    val success: Boolean,
    val coinsDelta: Long,
    /** Objeto que llegará a la mochila al abrir el juego de PC. */
    val itemCode: String?,
)

/**
 * Llamadas a las funciones de minijuegos del esquema `app`. Toda regla (intentos, recompensas,
 * precio mínimo de Vicente, saldo) la valida el servidor; tras cada partida se vuelve a
 * sincronizar la foto del jugador para ver monedas e intentos al día.
 */
@Singleton
class MinigameRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val players: PlayerRepository,
) {

    suspend fun playSalva(success: Boolean): MinigameReward {
        val response = rpc(
            "play_minigame_attempt",
            buildJsonObject {
                put("p_code", CODE_SALVA)
                put("p_result", if (success) "exito" else "fallo")
            },
        ).decodeAs<AttemptResponse>()
        players.sync(waitIfBusy = true)
        return MinigameReward(success, response.coinsAwarded, response.itemCode)
    }

    /** Abre una negociación o retoma la que quedó abierta (esa no gasta otro intento). */
    suspend fun startNegotiation(): Negotiation {
        val r = rpc("start_negotiation", JsonObject(emptyMap())).decodeAs<NegotiationResponse>()
        players.sync(waitIfBusy = true)
        return Negotiation(r.id, r.itemCode, r.askingPrice, r.patience, r.turnsLeft)
    }

    suspend fun makeOffer(negotiationId: Long, offer: Long): OfferResponse {
        val r = rpc(
            "make_offer",
            buildJsonObject {
                put("p_negotiation_id", negotiationId)
                put("p_offer", offer)
            },
        ).decodeAs<OfferRaw>()
        val response = when (r.status) {
            "aceptada" -> OfferResponse.Aceptada(r.itemCode.orEmpty(), r.finalPrice ?: offer)
            "se_retiro" -> OfferResponse.SeRetiro
            else -> OfferResponse.Contraoferta(r.askingPrice ?: 0, r.patience ?: 0, r.turnsLeft ?: 0)
        }
        if (response !is OfferResponse.Contraoferta) players.sync(waitIfBusy = true)
        return response
    }

    private suspend fun rpc(function: String, params: JsonObject) =
        supabase.postgrest.rpc(function, params) { schema = SCHEMA_APP }

    @Serializable
    private data class AttemptResponse(
        @SerialName("attempts_remaining") val attemptsRemaining: Int = 0,
        @SerialName("coins_awarded") val coinsAwarded: Long = 0,
        @SerialName("item_code") val itemCode: String? = null,
    )

    @Serializable
    private data class NegotiationResponse(
        @SerialName("negotiation_id") val id: Long,
        @SerialName("item_code") val itemCode: String,
        @SerialName("asking_price") val askingPrice: Long,
        val patience: Int = 100,
        @SerialName("turns_left") val turnsLeft: Int = 3,
    )

    @Serializable
    private data class OfferRaw(
        val status: String,
        @SerialName("asking_price") val askingPrice: Long? = null,
        val patience: Int? = null,
        @SerialName("turns_left") val turnsLeft: Int? = null,
        @SerialName("item_code") val itemCode: String? = null,
        @SerialName("final_price") val finalPrice: Long? = null,
    )

    companion object {
        const val SCHEMA_APP = "app"
        const val CODE_SALVA = "DESPIERTA_SALVA"
        const val CODE_VICENTE = "NEGOCIA_VICENTE"
    }
}
