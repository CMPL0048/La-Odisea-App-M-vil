package com.laodisea.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Todo lo que pinta la pantalla de Inicio; se guarda completo en DataStore para el modo sin conexión. */
@Serializable
data class HomeData(
    val snapshot: PlayerSnapshot,
    val minigames: List<MinigameStatus> = emptyList(),
    val latestNews: CampusNews? = null,
    /** Momento (epoch ms) en que se descargó del servidor. */
    val fetchedAt: Long = 0,
)

/** Fila de app.minigames. */
@Serializable
data class Minigame(
    val code: String,
    val name: String,
    @SerialName("attempts_per_cycle") val attemptsPerCycle: Int = 0,
    @SerialName("cycle_hours") val cycleHours: Int = 0,
)

/** Respuesta de app.get_minigame_state(p_code). */
@Serializable
data class MinigameState(
    @SerialName("attempts_remaining") val attemptsRemaining: Int = 0,
    @SerialName("next_reset_at") val nextResetAt: String? = null,
)

@Serializable
data class MinigameStatus(
    val code: String,
    val name: String,
    val attemptsRemaining: Int,
    val attemptsPerCycle: Int,
    /** ISO-8601 con zona horaria, tal como lo manda Postgres. */
    val nextResetAt: String? = null,
)

/** Fila de app.campus_news. */
@Serializable
data class CampusNews(
    val id: Long,
    val title: String,
    val summary: String? = null,
    val category: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
)
