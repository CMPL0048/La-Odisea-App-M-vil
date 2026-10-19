package com.laodisea.app.domain.minigames

import com.laodisea.app.domain.minigames.SalvaGame.Phase
import com.laodisea.app.domain.minigames.SalvaGame.TapResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SalvaGameTest {

    private fun newGame() = SalvaGame(Random(42)).apply { start() }

    /** Lleva el marcador al centro de la zona dorada avanzando el tiempo. */
    private fun SalvaGame.moveIntoZone() {
        val target = (state.zoneStart + state.zoneEnd) / 2
        repeat(10_000) {
            if (kotlin.math.abs(state.position - target) < 0.01f) return
            tick(0.005f)
        }
        error("El marcador nunca llegó a la zona")
    }

    private fun SalvaGame.moveOutOfZone() {
        repeat(10_000) {
            if (state.position !in (state.zoneStart - 0.02f)..(state.zoneEnd + 0.02f)) return
            tick(0.005f)
        }
        error("El marcador nunca salió de la zona")
    }

    @Test
    fun ignoresTapsBeforeStarting() {
        val game = SalvaGame(Random(1))
        assertEquals(TapResult.Ignorado, game.tap())
        assertEquals(Phase.Listo, game.state.phase)
    }

    @Test
    fun markerBouncesAndStaysInsideBar() {
        val game = newGame()
        repeat(500) {
            game.tick(0.37f)
            assertTrue(game.state.position in 0f..1f)
        }
        // Un paso enorme no saca al marcador de la barra.
        game.tick(10f)
        assertTrue(game.state.position in 0f..1f)
    }

    @Test
    fun threeHitsWakeSalva() {
        val game = newGame()
        repeat(SalvaGame.HITS_TO_WIN) {
            game.moveIntoZone()
            assertEquals(TapResult.Acierto, game.tap())
        }
        assertEquals(Phase.Ganado, game.state.phase)
        assertTrue(game.state.won)
        assertEquals(1f, game.state.wakefulness)
        // Ya terminó: más toques no cambian nada.
        assertEquals(TapResult.Ignorado, game.tap())
    }

    @Test
    fun threeMissesLose() {
        val game = newGame()
        repeat(SalvaGame.MAX_MISSES) {
            game.moveOutOfZone()
            assertEquals(TapResult.Fallo, game.tap())
        }
        assertEquals(Phase.Perdido, game.state.phase)
    }

    @Test
    fun eachHitMakesItHarder() {
        val game = newGame()
        val before = game.state
        game.moveIntoZone()
        game.tap()
        val after = game.state
        assertTrue(after.speed > before.speed)
        assertTrue(after.zoneWidth < before.zoneWidth)
        assertTrue(after.zoneStart >= 0f && after.zoneEnd <= 1f)
    }

    @Test
    fun zoneNeverShrinksBelowMinimum() {
        val game = newGame()
        repeat(SalvaGame.HITS_TO_WIN - 1) {
            game.moveIntoZone()
            game.tap()
        }
        assertTrue(game.state.zoneWidth >= SalvaGame.MIN_ZONE)
    }

    @Test
    fun abandonCountsAsLoss() {
        val game = newGame()
        game.abandon()
        assertEquals(Phase.Perdido, game.state.phase)
    }
}
