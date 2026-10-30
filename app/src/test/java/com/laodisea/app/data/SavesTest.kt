package com.laodisea.app.data

import com.laodisea.app.data.model.HomeData
import com.laodisea.app.data.model.LinkedSave
import com.laodisea.app.data.model.PlayerSnapshot
import com.laodisea.app.data.model.SaveSummary
import com.laodisea.app.ui.util.Formats
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Respuestas de ejemplo del contrato de la migración 08 (partidas vinculadas por QR). */
class SavesTest {

    private fun resource(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

    private val saves = OdiseaJson.decodeFromString(ListSerializer(SaveSummary.serializer()), resource("list_my_saves_demo.json"))
    private val active = OdiseaJson.decodeFromString(PlayerSnapshot.serializer(), resource("active_snapshot_demo.json"))

    @Test
    fun decodesListMySaves() {
        assertEquals(3, saves.size)
        val principal = saves.first()
        assertTrue(principal.isPrincipal)
        assertFalse(principal.isActive)
        assertNull(principal.linkedAt)
        assertEquals("Universidad Vizcaya de las Américas", principal.zoneName)
        assertEquals(5400L, principal.playtimeSeconds)

        val linked = saves[1]
        assertTrue(linked.isActive)
        assertEquals("PC del laboratorio", linked.name)
        assertEquals(1, linked.level)

        // Una partida recién creada en la PC llega sin datos de juego.
        val empty = saves[2]
        assertNull(empty.level)
        assertNull(empty.zoneName)
        assertNull(empty.lastPlayed)
    }

    @Test
    fun decodesActiveSnapshotWithSave() {
        assertEquals("PC del laboratorio", active.save?.name)
        assertFalse(active.save!!.isPrincipal)
        // El resto es igual a get_player_snapshot.
        assertEquals(120L, active.coins)
        assertEquals("VIZCAYA", active.currentZone?.code)
    }

    @Test
    fun legacySnapshotHasNoSave() {
        val legacy = OdiseaJson.decodeFromString(PlayerSnapshot.serializer(), resource("player_snapshot_demo.json"))
        assertNull(legacy.save)
        assertNull(HomeData(legacy).activeSaveName)
    }

    @Test
    fun activeSaveNamePrefersSnapshot() {
        assertEquals("PC del laboratorio", HomeData(active, saves = saves).activeSaveName)
        val withoutSave = active.copy(save = null)
        assertEquals("PC del laboratorio", HomeData(withoutSave, saves = saves).activeSaveName)
    }

    @Test
    fun savesSurviveCacheRoundTrip() {
        val home = HomeData(active, saves = saves, fetchedAt = 1)
        val encoded = OdiseaJson.encodeToString(HomeData.serializer(), home)
        assertEquals(home, OdiseaJson.decodeFromString(HomeData.serializer(), encoded))
    }

    @Test
    fun decodesLinkSave() {
        val linked = OdiseaJson.decodeFromString(
            LinkedSave.serializer(),
            """{"save_id":"5b0f6a0e-2c1d-4f7e-9a3b-6d2e8c4f1a90","name":"PC del laboratorio","device_name":null}""",
        )
        assertEquals("PC del laboratorio", linked.name)
        assertNull(linked.deviceName)
    }

    @Test
    fun serverSaveMessagesPassThrough() {
        assertEquals(
            "Ya tienes 5 partidas. Desvincula una para agregar otra.",
            Exception("Ya tienes 5 partidas. Desvincula una para agregar otra").toUserMessage(),
        )
        assertEquals(
            "Esta partida ya está vinculada a una cuenta.",
            Exception("P0001: Esta partida ya está vinculada a una cuenta\nHint: …").toUserMessage(),
        )
    }

    @Test
    fun detectsMissingFunction() {
        assertTrue(Exception("PGRST202: Could not find the function app.get_active_snapshot without parameters").isMissingFunction())
        assertTrue(Exception("42883: function app.list_my_saves() does not exist").isMissingFunction())
        assertFalse(Exception("Sin intentos disponibles").isMissingFunction())
    }

    @Test
    fun formatsLastPlayed() {
        val now = Instant.parse("2026-10-30T18:00:00Z")
        assertEquals("hace un momento", Formats.timeAgo("2026-10-30T17:59:40Z", now))
        assertEquals("hace 25 min", Formats.timeAgo("2026-10-30T17:35:00Z", now))
        assertEquals("hace 3 h", Formats.timeAgo("2026-10-30T15:00:00+00:00", now))
        assertEquals("hace 1 día", Formats.timeAgo("2026-10-29T12:00:00Z", now))
        assertEquals("hace 4 días", Formats.timeAgo("2026-10-26T12:00:00Z", now))
        assertNull(Formats.timeAgo(null, now))
    }
}
