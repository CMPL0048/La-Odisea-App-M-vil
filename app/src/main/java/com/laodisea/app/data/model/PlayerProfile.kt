package com.laodisea.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fila propia de `public.players` (nombre y avatar). Se lee aparte porque `get_player_snapshot`
 * no devuelve `avatar_url`. Ver `domain/Profile.kt` para el formato del avatar.
 */
@Serializable
data class PlayerProfile(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)
