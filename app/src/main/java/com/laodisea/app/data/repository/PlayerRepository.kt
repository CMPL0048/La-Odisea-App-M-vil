package com.laodisea.app.data.repository

import com.laodisea.app.data.isNetworkError
import com.laodisea.app.data.local.PlayerCache
import com.laodisea.app.data.model.CampusNews
import com.laodisea.app.data.model.CatalogData
import com.laodisea.app.data.model.CatalogItem
import com.laodisea.app.data.model.GameCharacter
import com.laodisea.app.data.model.HomeData
import com.laodisea.app.data.model.Minigame
import com.laodisea.app.data.model.MinigameState
import com.laodisea.app.data.model.MinigameStatus
import com.laodisea.app.data.model.PlayerSnapshot
import com.laodisea.app.data.model.Zone
import com.laodisea.app.data.toUserMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de la última sincronización con el servidor, compartido por todas las pantallas. */
data class SyncStatus(
    val refreshing: Boolean = false,
    /** La última sincronización falló por falta de red: se muestran los datos guardados. */
    val offline: Boolean = false,
    val error: String? = null,
)

@Singleton
class PlayerRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val auth: AuthRepository,
    private val cache: PlayerCache,
) {
    /** Última foto guardada; emite de nuevo cada vez que se actualiza. */
    val cachedHome: Flow<HomeData?> = cache.homeData

    /** Catálogos guardados (objetos, personajes, zonas). */
    val cachedCatalog: Flow<CatalogData?> = cache.catalog

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val syncMutex = Mutex()

    /**
     * Descarga la foto del jugador y los catálogos y los guarda. Si ya hay una sincronización en
     * curso no lanza otra (salvo con [waitIfBusy], que espera y vuelve a descargar).
     * Nunca lanza excepción: el resultado queda en [syncStatus].
     */
    suspend fun sync(waitIfBusy: Boolean = false) {
        if (waitIfBusy) syncMutex.lock() else if (!syncMutex.tryLock()) return
        try {
            _syncStatus.update { it.copy(refreshing = true, error = null) }
            val result = runCatching {
                coroutineScope {
                    val home = async { refreshHome() }
                    val catalog = async { refreshCatalog() }
                    home.await()
                    catalog.await()
                }
            }
            _syncStatus.value = result.fold(
                onSuccess = { SyncStatus() },
                onFailure = { SyncStatus(offline = it.isNetworkError(), error = it.toUserMessage()) },
            )
        } finally {
            syncMutex.unlock()
        }
    }

    /**
     * Descarga el snapshot del jugador, el estado de los minijuegos y la última noticia,
     * y lo guarda en DataStore. Lanza excepción si no hay red o falla el servidor.
     */
    suspend fun refreshHome(): HomeData = coroutineScope {
        val snapshot = async { fetchSnapshot() }
        val minigames = async { fetchMinigames() }
        val news = async { runCatching { fetchNews() }.getOrNull() }
        // Si fallan solo las noticias se conservan las que ya estaban guardadas.
        val newsList = news.await() ?: cache.read()?.news.orEmpty()
        val data = HomeData(
            snapshot = snapshot.await(),
            minigames = minigames.await(),
            latestNews = newsList.firstOrNull(),
            news = newsList,
            fetchedAt = System.currentTimeMillis(),
        )
        val owner = auth.currentUserId ?: data.snapshot.player?.id
        if (owner != null) cache.save(owner, data)
        data
    }

    suspend fun refreshCatalog(): CatalogData = coroutineScope {
        val items = async { supabase.postgrest.from("items").select().decodeList<CatalogItem>() }
        val characters = async { supabase.postgrest.from("characters").select().decodeList<GameCharacter>() }
        val zones = async {
            supabase.postgrest.from("zones")
                .select { order("order_index", Order.ASCENDING) }
                .decodeList<Zone>()
        }
        val data = CatalogData(items.await(), characters.await(), zones.await(), System.currentTimeMillis())
        cache.saveCatalog(data)
        data
    }

    /** Cambia el nombre de jugador (RLS solo deja editar la fila propia) y vuelve a sincronizar. */
    suspend fun updateDisplayName(name: String) {
        val uid = auth.currentUserId ?: error("No autenticado")
        supabase.postgrest.from("players").update(buildJsonObject { put("display_name", name.trim()) }) {
            filter { eq("id", uid) }
        }
        sync(waitIfBusy = true)
    }

    private suspend fun fetchSnapshot(): PlayerSnapshot =
        supabase.postgrest.rpc(FN_SNAPSHOT).decodeAs<PlayerSnapshot>()

    private suspend fun fetchMinigames(): List<MinigameStatus> = coroutineScope {
        val catalog = supabase.postgrest.from(SCHEMA_APP, "minigames")
            .select()
            .decodeList<Minigame>()
        catalog.map { game ->
            async {
                val state = supabase.postgrest.rpc(
                    function = FN_MINIGAME_STATE,
                    parameters = buildJsonObject { put("p_code", game.code) },
                ) { schema = SCHEMA_APP }.decodeAs<MinigameState>()
                MinigameStatus(
                    code = game.code,
                    name = game.name,
                    attemptsRemaining = state.attemptsRemaining,
                    attemptsPerCycle = game.attemptsPerCycle,
                    nextResetAt = state.nextResetAt,
                )
            }
        }.awaitAll()
    }

    private suspend fun fetchNews(): List<CampusNews> =
        supabase.postgrest.from(SCHEMA_APP, "campus_news")
            .select {
                order("published_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                limit(NEWS_LIMIT)
            }
            .decodeList<CampusNews>()

    private companion object {
        const val SCHEMA_APP = "app"
        const val NEWS_LIMIT = 50L
        const val FN_SNAPSHOT = "get_player_snapshot"
        const val FN_MINIGAME_STATE = "get_minigame_state"
    }
}
