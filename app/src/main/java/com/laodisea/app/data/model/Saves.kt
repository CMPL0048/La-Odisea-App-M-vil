package com.laodisea.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Elemento de `app.list_my_saves()`: la partida principal de la cuenta o una vinculada por QR. */
@Serializable
data class SaveSummary(
    @SerialName("save_id") val saveId: String,
    val name: String,
    /** La partida de la propia cuenta: siempre está, no se renombra ni se desvincula. */
    @SerialName("is_principal") val isPrincipal: Boolean = false,
    /** La que se ve en la app y a la que llegan los premios de los minijuegos. */
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("linked_at") val linkedAt: String? = null,
    val level: Int? = null,
    @SerialName("zone_code") val zoneCode: String? = null,
    @SerialName("zone_name") val zoneName: String? = null,
    @SerialName("playtime_seconds") val playtimeSeconds: Long? = null,
    val coins: Long? = null,
    @SerialName("last_played") val lastPlayed: String? = null,
)

/** Campo `save` de `app.get_active_snapshot()`: de qué partida es la foto. */
@Serializable
data class ActiveSave(
    @SerialName("save_id") val saveId: String,
    val name: String,
    @SerialName("is_principal") val isPrincipal: Boolean = true,
)

/** Respuesta de `app.link_save(p_code)`. */
@Serializable
data class LinkedSave(
    @SerialName("save_id") val saveId: String,
    val name: String,
    @SerialName("device_name") val deviceName: String? = null,
)
