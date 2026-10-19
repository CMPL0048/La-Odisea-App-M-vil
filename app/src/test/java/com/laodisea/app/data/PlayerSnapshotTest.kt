package com.laodisea.app.data

import com.laodisea.app.data.model.HomeData
import com.laodisea.app.data.model.MinigameState
import com.laodisea.app.data.model.MinigameStatus
import com.laodisea.app.data.model.PlayerSnapshot
import com.laodisea.app.ui.home.JuanmaTips
import com.laodisea.app.ui.util.Formats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Usa la respuesta real de get_player_snapshot de la cuenta demo (18 oct 2026). */
class PlayerSnapshotTest {

    private val raw = javaClass.classLoader!!.getResource("player_snapshot_demo.json")!!.readText()
    private val snapshot = OdiseaJson.decodeFromString(PlayerSnapshot.serializer(), raw)

    @Test
    fun decodesDemoSnapshot() {
        assertEquals("tic-310114", snapshot.player?.displayName)
        assertNull(snapshot.player?.avatarUrl)
        assertEquals(120L, snapshot.coins)
        assertEquals(3, snapshot.stats?.level)
        assertEquals(85, snapshot.stats?.health)
        assertEquals(120, snapshot.stats?.healthMax)
        assertEquals(4, snapshot.zones.size)
        assertEquals(5, snapshot.inventory.size)
        assertEquals(3, snapshot.inventory.first { it.code == "ESPATULA" }.quantity)
        assertEquals(12, snapshot.capacity)
        assertEquals(6, snapshot.bestiary.size)
    }

    @Test
    fun currentZoneAndProgress() {
        assertEquals("VIZCAYA", snapshot.currentZone?.code)
        // (3 + 2) de (3 + 6 + 1 + 0) subniveles
        assertEquals(0.5f, snapshot.progress, 0.0001f)
        assertEquals(1, snapshot.completedZones)
    }

    @Test
    fun homeDataSurvivesCacheRoundTrip() {
        val home = HomeData(
            snapshot = snapshot,
            minigames = listOf(MinigameStatus("DESPIERTA_SALVA", "Despierta a Salva", 3, 4, "2026-10-19T08:23:41.038696+00:00")),
            latestNews = null,
            fetchedAt = 1_760_000_000_000,
        )
        val encoded = OdiseaJson.encodeToString(HomeData.serializer(), home)
        assertEquals(home, OdiseaJson.decodeFromString(HomeData.serializer(), encoded))
    }

    @Test
    fun decodesMinigameState() {
        val state = OdiseaJson.decodeFromString(
            MinigameState.serializer(),
            """{"next_reset_at":"2026-10-19T08:23:41.038696+00:00","attempts_remaining":4}""",
        )
        assertEquals(4, state.attemptsRemaining)
        assertTrue(Formats.untilReset(state.nextResetAt).isNotBlank())
    }

    @Test
    fun juanmaTipFollowsZone() {
        val tip = JuanmaTips.forHome(HomeData(snapshot))
        assertTrue(tip, tip.contains("Vicente"))
    }

    @Test
    fun formatsPlaytime() {
        assertEquals("1 h 30 min", Formats.playtime(5400))
        assertEquals("25 min", Formats.playtime(1500))
    }
}
