package com.laodisea.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.laodisea.app.data.model.CatalogData
import com.laodisea.app.data.model.HomeData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda la última «foto» del jugador (snapshot + minijuegos + noticia) y los catálogos
 * para mostrarlos sin conexión.
 */
@Singleton
class PlayerCache @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    val homeData: Flow<HomeData?> = dataStore.data.map { prefs -> prefs[HOME_DATA]?.let { decode(HomeData.serializer(), it) } }

    val catalog: Flow<CatalogData?> = dataStore.data.map { prefs -> prefs[CATALOG]?.let { decode(CatalogData.serializer(), it) } }

    suspend fun read(): HomeData? = homeData.first()

    /** Usuario dueño de la caché, para no mostrar datos de otra cuenta. */
    suspend fun ownerId(): String? = dataStore.data.first()[OWNER_ID]

    suspend fun save(ownerId: String, data: HomeData) {
        dataStore.edit { prefs ->
            prefs[OWNER_ID] = ownerId
            prefs[HOME_DATA] = json.encodeToString(HomeData.serializer(), data)
        }
    }

    suspend fun saveCatalog(data: CatalogData) {
        dataStore.edit { prefs -> prefs[CATALOG] = json.encodeToString(CatalogData.serializer(), data) }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(OWNER_ID)
            prefs.remove(HOME_DATA)
            // Los personajes secretos dependen de la cuenta, así que el catálogo también se borra.
            prefs.remove(CATALOG)
        }
    }

    private fun <T> decode(serializer: KSerializer<T>, raw: String): T? =
        runCatching { json.decodeFromString(serializer, raw) }.getOrNull()

    private companion object {
        val OWNER_ID = stringPreferencesKey("cache_owner_id")
        val HOME_DATA = stringPreferencesKey("home_data")
        val CATALOG = stringPreferencesKey("catalog_data")
    }
}
