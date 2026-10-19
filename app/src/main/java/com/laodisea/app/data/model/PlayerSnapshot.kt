package com.laodisea.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta de la RPC public.get_player_snapshot(). */
@Serializable
data class PlayerSnapshot(
    val player: Player? = null,
    val stats: PlayerStats? = null,
    val coins: Long = 0,
    val zones: List<ZoneProgress> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val capacity: Int = 0,
    val bestiary: List<String> = emptyList(),
    @SerialName("pending_rewards") val pendingRewards: Int = 0,
) {
    /** Zona en la que está el jugador: la de sus stats o, si falta, la marcada como actual. */
    val currentZone: ZoneProgress?
        get() = zones.firstOrNull { it.code == stats?.currentZoneCode }
            ?: zones.firstOrNull { it.status == ZoneProgress.STATUS_CURRENT }

    /** Avance global por subniveles completados de las zonas visibles (0..1). */
    val progress: Float
        get() {
            val total = zones.sumOf { it.sublevelsTotal }
            if (total == 0) return 0f
            val done = zones.sumOf { it.sublevelsCompleted.coerceAtMost(it.sublevelsTotal) }
            return done.toFloat() / total
        }

    val completedZones: Int get() = zones.count { it.status == ZoneProgress.STATUS_COMPLETED }
}

@Serializable
data class Player(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class PlayerStats(
    val level: Int = 1,
    val xp: Int = 0,
    val health: Int = 0,
    @SerialName("health_max") val healthMax: Int = 0,
    val resistance: Int = 0,
    @SerialName("resistance_max") val resistanceMax: Int = 0,
    @SerialName("enemies_defeated") val enemiesDefeated: Int = 0,
    @SerialName("playtime_seconds") val playtimeSeconds: Long = 0,
    @SerialName("current_zone_code") val currentZoneCode: String? = null,
)

@Serializable
data class ZoneProgress(
    val code: String,
    val name: String,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("subniveles_total") val sublevelsTotal: Int = 0,
    val status: String = STATUS_LOCKED,
    @SerialName("subniveles_completados") val sublevelsCompleted: Int = 0,
    @SerialName("route_unlocked") val routeUnlocked: Boolean = false,
) {
    companion object {
        const val STATUS_LOCKED = "bloqueado"
        const val STATUS_CURRENT = "actual"
        const val STATUS_COMPLETED = "completado"
    }
}

@Serializable
data class InventoryItem(
    val code: String,
    val name: String,
    val category: String? = null,
    val rarity: String? = null,
    val description: String? = null,
    @SerialName("effect_description") val effectDescription: String? = null,
    val quantity: Int = 0,
)
