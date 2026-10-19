package com.laodisea.app.domain.minigames

import kotlin.random.Random

/**
 * «Despierta a Salva»: una barra de precisión. El marcador va y viene; el jugador toca cuando
 * pasa por la zona dorada. Con [HITS_TO_WIN] aciertos Salva despierta; con [MAX_MISSES] fallos
 * se rinde. Cada acierto acelera el marcador y achica la zona.
 *
 * Es Kotlin puro (sin Android) para probarlo con JUnit; la pantalla solo dibuja [state] y llama
 * a [tick] en cada cuadro y a [tap] cuando el jugador toca. El resultado se manda al servidor,
 * que es quien decide la recompensa.
 */
class SalvaGame(private val random: Random = Random.Default) {

    enum class Phase { Listo, Jugando, Ganado, Perdido }

    enum class TapResult { Acierto, Fallo, Ignorado }

    data class State(
        val phase: Phase = Phase.Listo,
        /** Posición del marcador, de 0 a 1. */
        val position: Float = 0f,
        /** +1 hacia la derecha, -1 hacia la izquierda. */
        val direction: Int = 1,
        val zoneStart: Float = 0.39f,
        val zoneWidth: Float = INITIAL_ZONE,
        /** Unidades de barra por segundo. */
        val speed: Float = INITIAL_SPEED,
        val hits: Int = 0,
        val misses: Int = 0,
        val lastTap: TapResult? = null,
    ) {
        val zoneEnd: Float get() = zoneStart + zoneWidth
        val finished: Boolean get() = phase == Phase.Ganado || phase == Phase.Perdido
        val won: Boolean get() = phase == Phase.Ganado
        /** Qué tan despierto está Salva (0..1), para la ilustración. */
        val wakefulness: Float get() = hits.toFloat() / HITS_TO_WIN
    }

    var state: State = State()
        private set

    fun start() {
        state = State(phase = Phase.Jugando, zoneStart = randomZoneStart(INITIAL_ZONE))
    }

    /** Avanza el marcador [seconds] segundos, rebotando en los extremos. */
    fun tick(seconds: Float) {
        val s = state
        if (s.phase != Phase.Jugando || seconds <= 0f) return
        var pos = s.position + s.direction * s.speed * seconds
        var dir = s.direction
        // Rebota tantas veces como haga falta si el paso es grande.
        while (pos > 1f || pos < 0f) {
            if (pos > 1f) { pos = 2f - pos; dir = -1 }
            if (pos < 0f) { pos = -pos; dir = 1 }
        }
        state = s.copy(position = pos, direction = dir)
    }

    fun tap(): TapResult {
        val s = state
        if (s.phase != Phase.Jugando) return TapResult.Ignorado
        val hit = s.position in s.zoneStart..s.zoneEnd
        state = if (hit) {
            val hits = s.hits + 1
            val width = (s.zoneWidth - ZONE_SHRINK).coerceAtLeast(MIN_ZONE)
            s.copy(
                hits = hits,
                phase = if (hits >= HITS_TO_WIN) Phase.Ganado else Phase.Jugando,
                zoneWidth = width,
                zoneStart = randomZoneStart(width),
                speed = s.speed + SPEED_STEP,
                lastTap = TapResult.Acierto,
            )
        } else {
            val misses = s.misses + 1
            s.copy(
                misses = misses,
                phase = if (misses >= MAX_MISSES) Phase.Perdido else Phase.Jugando,
                lastTap = TapResult.Fallo,
            )
        }
        return state.lastTap!!
    }

    /** El jugador sale a media partida: cuenta como fallo. */
    fun abandon() {
        if (state.phase == Phase.Jugando) state = state.copy(phase = Phase.Perdido)
    }

    private fun randomZoneStart(width: Float): Float =
        MARGIN + random.nextFloat() * (1f - 2 * MARGIN - width)

    companion object {
        const val HITS_TO_WIN = 3
        const val MAX_MISSES = 3
        const val INITIAL_SPEED = 0.7f
        const val SPEED_STEP = 0.3f
        const val INITIAL_ZONE = 0.22f
        const val ZONE_SHRINK = 0.05f
        const val MIN_ZONE = 0.08f
        private const val MARGIN = 0.05f
    }
}
