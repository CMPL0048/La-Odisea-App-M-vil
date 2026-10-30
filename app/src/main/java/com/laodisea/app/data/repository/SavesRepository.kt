package com.laodisea.app.data.repository

import com.laodisea.app.data.local.PlayerCache
import com.laodisea.app.data.model.LinkedSave
import com.laodisea.app.data.model.SaveSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Partidas de la cuenta: la principal y las del juego de PC vinculadas por QR. La lista se
 * descarga con la sincronización de [PlayerRepository] y vive en la misma caché. Los errores del
 * servidor ya vienen en español y [com.laodisea.app.data.toUserMessage] los deja pasar.
 */
@Singleton
class SavesRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val players: PlayerRepository,
    private val cache: PlayerCache,
) {
    /** Partidas guardadas, la principal primero. */
    val saves: Flow<List<SaveSummary>> = players.cachedHome.map { it?.saves.orEmpty() }

    /** Vincula la partida del código (ya normalizado); queda como la activa. */
    suspend fun link(code: String): LinkedSave {
        val linked = rpc(FN_LINK, buildJsonObject { put("p_code", code) }).decodeAs<LinkedSave>()
        reloadActiveSave()
        return linked
    }

    /** Cambia la partida que se ve y a la que llegan los premios. */
    suspend fun setActive(saveId: String) {
        rpc(FN_SET_ACTIVE, buildJsonObject { put("p_save_id", saveId) })
        reloadActiveSave()
    }

    suspend fun rename(saveId: String, name: String) {
        rpc(
            FN_RENAME,
            buildJsonObject {
                put("p_save_id", saveId)
                put("p_name", name.trim())
            },
        )
        players.sync(waitIfBusy = true)
    }

    /** Quita la partida de la cuenta. No borra su progreso: sigue en la PC y se puede volver a vincular. */
    suspend fun unlink(saveId: String) {
        rpc(FN_UNLINK, buildJsonObject { put("p_save_id", saveId) })
        reloadActiveSave()
    }

    // La foto guardada puede ser de otra partida: se marca como vieja y se vuelve a descargar.
    private suspend fun reloadActiveSave() {
        cache.updateHome { it.copy(fetchedAt = 0) }
        players.sync(waitIfBusy = true)
    }

    private suspend fun rpc(function: String, params: JsonObject) =
        supabase.postgrest.rpc(function, params) { schema = SCHEMA_APP }

    private companion object {
        const val SCHEMA_APP = "app"
        const val FN_LINK = "link_save"
        const val FN_SET_ACTIVE = "set_active_save"
        const val FN_RENAME = "rename_save"
        const val FN_UNLINK = "unlink_save"
    }
}
