package com.laodisea.app.domain

import com.laodisea.app.data.OdiseaJson
import com.laodisea.app.data.model.CatalogData
import com.laodisea.app.data.model.InventoryItem
import com.laodisea.app.data.model.PlayerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pantallas de lectura con los datos reales de la cuenta demo. El catálogo viene tal como lo
 * devuelve Supabase a esa cuenta: sin Toño, que es secreto y aún no lo desbloquea.
 */
class GameViewsTest {

    private fun resource(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()
    private val snapshot = OdiseaJson.decodeFromString(PlayerSnapshot.serializer(), resource("player_snapshot_demo.json"))
    private val catalog = OdiseaJson.decodeFromString(CatalogData.serializer(), resource("catalog_demo.json"))

    @Test
    fun mapShowsVisitedCurrentLockedAndHiddenZones() {
        val zones = mapZones(snapshot, catalog)
        assertEquals(listOf("UT", "VIZCAYA", "UNIVA", "UAN", "TEC"), zones.map { it.code })
        assertEquals(MapZoneState.Completada, zones[0].state)
        assertEquals(MapZoneState.Actual, zones[1].state)
        assertEquals("Vicente el Águila", zones[1].bossName)
        assertEquals(2, zones[1].sublevelsCompleted)
        assertEquals(MapZoneState.Bloqueada, zones[2].state)
        // El TEC está oculto: no trae nombre real ni jefe.
        assertEquals(MapZoneState.Oculta, zones[4].state)
        assertEquals("Campus misterioso", zones[4].name)
        assertNull(zones[4].bossName)
        assertTrue(snapshot.canSeeMap())
    }

    @Test
    fun backpackFillsCapacityWithEmptySlots() {
        val slots = backpackSlots(snapshot)
        assertEquals(12, slots.size)
        assertEquals(5, slots.count { it != null })
        assertEquals(7, slots.count { it == null })
    }

    @Test
    fun itemDetailMergesInventoryAndCatalog() {
        val espatula = itemDetail("ESPATULA", snapshot, catalog)!!
        assertEquals(3, espatula.quantity)
        assertEquals(50L, espatula.price)
        val muestrario = itemDetail("MUESTRARIO", snapshot, catalog)!!
        assertEquals(0, muestrario.quantity)
        assertNull(itemDetail("NO_EXISTE", snapshot, catalog))
    }

    @Test
    fun showcaseHidesUnownedItemsWithoutMuestrario() {
        val entries = showcase(snapshot, catalog).flatMap { it.entries }
        assertEquals(6, entries.size)
        val muestrario = entries.first { it.item.code == "MUESTRARIO" }
        assertFalse(muestrario.owned)
        assertFalse(muestrario.revealed)
        assertTrue(entries.filter { it.owned }.all { it.revealed })
        assertEquals("clave", showcase(snapshot, catalog).first().category)
    }

    @Test
    fun showcaseRevealsEverythingWithMuestrario() {
        val withShowcase = snapshot.copy(
            inventory = snapshot.inventory + InventoryItem("MUESTRARIO", "Muestrario de materiales", quantity = 1),
        )
        assertTrue(showcase(withShowcase, catalog).flatMap { it.entries }.all { it.revealed })
    }

    @Test
    fun bestiaryListsUnlockedFirst() {
        val entries = bestiary(snapshot, catalog)
        assertEquals(8, entries.size)
        assertEquals(6, entries.count { it.unlocked })
        assertEquals("VICTOR", entries.first().character.code)
        assertFalse(entries.last().unlocked)
    }

    @Test
    fun statsSummaryForDemo() {
        val s = statsSummary(snapshot, catalog)
        assertEquals(1, s.zonesCompleted)
        assertEquals(4, s.zonesTotal)
        assertEquals(5, s.sublevelsCompleted)
        assertEquals(10, s.sublevelsTotal)
        assertEquals(5, s.itemsCarried)
        assertEquals(12, s.capacity)
        assertEquals(6, s.charactersUnlocked)
        assertEquals(8, s.charactersTotal)
    }

    @Test
    fun catalogSurvivesCacheRoundTrip() {
        val encoded = OdiseaJson.encodeToString(CatalogData.serializer(), catalog)
        assertEquals(catalog, OdiseaJson.decodeFromString(CatalogData.serializer(), encoded))
    }
}
