package com.laodisea.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Catálogos del juego (objetos, personajes y zonas); se guardan junto a la foto del jugador. */
@Serializable
data class CatalogData(
    val items: List<CatalogItem> = emptyList(),
    val characters: List<GameCharacter> = emptyList(),
    val zones: List<Zone> = emptyList(),
    val fetchedAt: Long = 0,
)

/** Fila de public.items. */
@Serializable
data class CatalogItem(
    val code: String,
    val name: String,
    val category: String? = null,
    val rarity: String? = null,
    val description: String? = null,
    @SerialName("effect_description") val effectDescription: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    val price: Long? = null,
)

/** Fila de public.characters (los secretos solo llegan si el jugador ya los desbloqueó). */
@Serializable
data class GameCharacter(
    val code: String,
    val name: String,
    val university: String? = null,
    val role: String? = null,
    val quote: String? = null,
    @SerialName("personality_traits") val personalityTraits: String? = null,
    @SerialName("first_encounter_zone") val firstEncounterZone: String? = null,
    @SerialName("illustration_url") val illustrationUrl: String? = null,
    @SerialName("is_secret") val isSecret: Boolean = false,
)

/** Fila de public.zones. */
@Serializable
data class Zone(
    val code: String,
    val name: String,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("subniveles_total") val sublevelsTotal: Int = 0,
    @SerialName("boss_character_code") val bossCharacterCode: String? = null,
    @SerialName("is_hidden_until_reached") val isHiddenUntilReached: Boolean = false,
)

/** Códigos de objetos que cambian lo que la app deja ver. */
object ItemCodes {
    const val MAPA = "MAPA"
    const val MUESTRARIO = "MUESTRARIO"
}
