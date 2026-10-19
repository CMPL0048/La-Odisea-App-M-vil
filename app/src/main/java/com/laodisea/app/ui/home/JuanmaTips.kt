package com.laodisea.app.ui.home

import com.laodisea.app.data.model.HomeData

/**
 * Consejo de Juanma según el estado del jugador. Es texto local para que funcione sin conexión;
 * cuando el catálogo tenga consejos en la base de datos se puede leer de ahí.
 */
object JuanmaTips {

    fun forHome(data: HomeData): String {
        val snapshot = data.snapshot
        val stats = snapshot.stats
        if (stats != null && stats.healthMax > 0 && stats.health * 100 / stats.healthMax < 30) {
            return "Tu vida anda baja. Antes de seguir explorando, busca algo para recuperarte."
        }
        if (snapshot.pendingRewards > 0) {
            return "Tienes recompensas esperando. Abre el juego en la PC y llegarán a tu mochila."
        }
        if (snapshot.capacity > 0 && snapshot.inventory.sumOf { it.quantity } >= snapshot.capacity) {
            return "Tu mochila está llena. Usa o suelta algo antes de recoger más objetos."
        }
        return byZone[snapshot.currentZone?.code]
            ?: data.minigames.firstOrNull { it.attemptsRemaining > 0 }
                ?.let { "Aún te quedan intentos en ${it.name}. ¡Aprovéchalos!" }
            ?: "Cada campus guarda secretos. Revisa bien cada subnivel antes de avanzar."
    }

    private val byZone = mapOf(
        "UT" to "Explora cada rincón de la UT: los subniveles esconden objetos para tu mochila.",
        "VIZCAYA" to "Vicente el Águila regatea duro. Junta monedas antes de ir con él.",
        "UNIVA" to "Lion el León no perdona descuidos. Llega con la vida al máximo.",
        "UAN" to "Elías el Ocelote es rápido. La resistencia será tu mejor aliada.",
        "TEC" to "Toño el Tigre es el último reto. Llega con la mochila bien surtida.",
    )
}
