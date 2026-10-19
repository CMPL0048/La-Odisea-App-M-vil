package com.laodisea.app.domain

import com.laodisea.app.data.model.CatalogData
import com.laodisea.app.data.model.CatalogItem
import com.laodisea.app.data.model.GameCharacter
import com.laodisea.app.data.model.InventoryItem
import com.laodisea.app.data.model.ItemCodes
import com.laodisea.app.data.model.PlayerSnapshot
import com.laodisea.app.data.model.ZoneProgress

/*
 * Funciones puras que cruzan la foto del jugador con los catálogos para las pantallas de lectura.
 * No dependen de Android para poder probarlas con JUnit.
 */

// ---------- Mapa ----------

enum class MapZoneState { Completada, Actual, Bloqueada, Oculta }

data class MapZone(
    val code: String,
    val name: String,
    val order: Int,
    val state: MapZoneState,
    val sublevelsCompleted: Int,
    val sublevelsTotal: Int,
    val routeUnlocked: Boolean,
    val bossName: String?,
)

/**
 * Zonas en orden de recorrido. Las ocultas que el jugador aún no alcanza vienen del catálogo
 * (la foto no las trae) y se muestran sin nombre.
 */
fun mapZones(snapshot: PlayerSnapshot, catalog: CatalogData?): List<MapZone> {
    val bosses = catalog?.characters.orEmpty().associateBy { it.code }
    val catalogZones = catalog?.zones.orEmpty().associateBy { it.code }
    val visible = snapshot.zones.map { zone ->
        MapZone(
            code = zone.code,
            name = zone.name,
            order = zone.orderIndex,
            state = when (zone.status) {
                ZoneProgress.STATUS_COMPLETED -> MapZoneState.Completada
                ZoneProgress.STATUS_CURRENT -> MapZoneState.Actual
                else -> MapZoneState.Bloqueada
            },
            sublevelsCompleted = zone.sublevelsCompleted.coerceAtMost(zone.sublevelsTotal),
            sublevelsTotal = zone.sublevelsTotal,
            routeUnlocked = zone.routeUnlocked,
            bossName = catalogZones[zone.code]?.bossCharacterCode?.let { bosses[it]?.name },
        )
    }
    val visibleCodes = visible.map { it.code }.toSet()
    val hidden = catalog?.zones.orEmpty()
        .filter { it.isHiddenUntilReached && it.code !in visibleCodes }
        .map { MapZone(it.code, "Campus misterioso", it.orderIndex, MapZoneState.Oculta, 0, it.sublevelsTotal, false, null) }
    return (visible + hidden).sortedBy { it.order }
}

fun PlayerSnapshot.hasItem(code: String): Boolean = inventory.any { it.code == code && it.quantity > 0 }

fun PlayerSnapshot.canSeeMap(): Boolean = hasItem(ItemCodes.MAPA)

// ---------- Mochila ----------

/** Casillas de la mochila: primero los objetos y después los huecos libres hasta la capacidad. */
fun backpackSlots(snapshot: PlayerSnapshot): List<InventoryItem?> {
    val items = snapshot.inventory.filter { it.quantity > 0 }
    val empty = (snapshot.capacity - items.size).coerceAtLeast(0)
    return items + List(empty) { null }
}

data class ItemDetail(
    val code: String,
    val name: String,
    val category: String?,
    val rarity: String?,
    val description: String?,
    val effect: String?,
    val price: Long?,
    val iconUrl: String?,
    val quantity: Int,
)

fun itemDetail(code: String, snapshot: PlayerSnapshot?, catalog: CatalogData?): ItemDetail? {
    val owned = snapshot?.inventory?.firstOrNull { it.code == code }
    val entry = catalog?.items?.firstOrNull { it.code == code }
    if (owned == null && entry == null) return null
    return ItemDetail(
        code = code,
        name = owned?.name ?: entry!!.name,
        category = owned?.category ?: entry?.category,
        rarity = owned?.rarity ?: entry?.rarity,
        description = owned?.description ?: entry?.description,
        effect = owned?.effectDescription ?: entry?.effectDescription,
        price = entry?.price,
        iconUrl = entry?.iconUrl,
        quantity = owned?.quantity ?: 0,
    )
}

// ---------- Muestrario ----------

data class ShowcaseEntry(
    val item: CatalogItem,
    val owned: Boolean,
    /** Sin el Muestrario de materiales solo se ve la información de lo que ya se tiene. */
    val revealed: Boolean,
)

data class ShowcaseSection(val category: String?, val entries: List<ShowcaseEntry>)

fun showcase(snapshot: PlayerSnapshot, catalog: CatalogData?): List<ShowcaseSection> {
    val hasShowcase = snapshot.hasItem(ItemCodes.MUESTRARIO)
    val ownedCodes = snapshot.inventory.filter { it.quantity > 0 }.map { it.code }.toSet()
    return catalog?.items.orEmpty()
        .map { ShowcaseEntry(it, owned = it.code in ownedCodes, revealed = hasShowcase || it.code in ownedCodes) }
        .groupBy { it.item.category }
        .map { (category, entries) -> ShowcaseSection(category, entries.sortedBy { it.item.name }) }
        .sortedBy { categoryOrder.indexOf(it.category).let { i -> if (i < 0) Int.MAX_VALUE else i } }
}

private val categoryOrder = listOf("clave", "equipo", "curacion")

// ---------- Bestiario ----------

data class BestiaryEntry(val character: GameCharacter, val unlocked: Boolean)

/** Todos los personajes visibles; los secretos solo aparecen si el servidor ya los entregó. */
fun bestiary(snapshot: PlayerSnapshot, catalog: CatalogData?): List<BestiaryEntry> {
    val unlocked = snapshot.bestiary.toSet()
    return catalog?.characters.orEmpty()
        .filter { !it.isSecret || it.code in unlocked }
        .map { BestiaryEntry(it, it.code in unlocked) }
        .sortedWith(compareBy({ !it.unlocked }, { roleOrder.indexOf(it.character.role) }, { it.character.name }))
}

private val roleOrder = listOf("protagonista", "aliado", "jefe")

// ---------- Estadísticas ----------

data class StatsSummary(
    val level: Int,
    val xp: Int,
    val health: Int,
    val healthMax: Int,
    val resistance: Int,
    val resistanceMax: Int,
    val enemiesDefeated: Int,
    val playtimeSeconds: Long,
    val coins: Long,
    val zonesCompleted: Int,
    val zonesTotal: Int,
    val sublevelsCompleted: Int,
    val sublevelsTotal: Int,
    val itemsCarried: Int,
    val capacity: Int,
    val charactersUnlocked: Int,
    /** Total conocido: el secreto no cuenta hasta que se desbloquea. */
    val charactersTotal: Int,
)

fun statsSummary(snapshot: PlayerSnapshot, catalog: CatalogData?): StatsSummary {
    val stats = snapshot.stats
    // Los campus ocultos no cuentan hasta alcanzarlos (igual que en Inicio y Mapa).
    val totalZones = snapshot.zones.size
    return StatsSummary(
        level = stats?.level ?: 1,
        xp = stats?.xp ?: 0,
        health = stats?.health ?: 0,
        healthMax = stats?.healthMax ?: 0,
        resistance = stats?.resistance ?: 0,
        resistanceMax = stats?.resistanceMax ?: 0,
        enemiesDefeated = stats?.enemiesDefeated ?: 0,
        playtimeSeconds = stats?.playtimeSeconds ?: 0,
        coins = snapshot.coins,
        zonesCompleted = snapshot.completedZones,
        zonesTotal = totalZones,
        sublevelsCompleted = snapshot.zones.sumOf { it.sublevelsCompleted.coerceAtMost(it.sublevelsTotal) },
        sublevelsTotal = snapshot.zones.sumOf { it.sublevelsTotal },
        itemsCarried = snapshot.inventory.count { it.quantity > 0 },
        capacity = snapshot.capacity,
        charactersUnlocked = snapshot.bestiary.size,
        charactersTotal = bestiary(snapshot, catalog).size.coerceAtLeast(snapshot.bestiary.size),
    )
}
