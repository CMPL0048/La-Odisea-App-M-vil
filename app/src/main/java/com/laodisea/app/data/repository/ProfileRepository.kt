package com.laodisea.app.data.repository

import com.laodisea.app.data.local.PlayerCache
import com.laodisea.app.data.model.PlayerProfile
import com.laodisea.app.domain.AVATAR_BUCKET
import com.laodisea.app.domain.photoAvatarPath
import com.laodisea.app.domain.photoAvatarValue
import com.laodisea.app.domain.presetAvatarValue
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Perfil del jugador: nombre y avatar. El perfil se descarga con la sincronización de
 * [PlayerRepository] y vive en la misma caché, así que también se ve sin conexión.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val auth: AuthRepository,
    private val players: PlayerRepository,
    private val cache: PlayerCache,
) {
    /** Perfil guardado. Si aún no se descarga, el nombre sale de la foto del jugador. */
    val profile: Flow<PlayerProfile?> = players.cachedHome.map { home ->
        home?.let { it.profile ?: PlayerProfile(displayName = it.snapshot.player?.displayName) }
    }

    val email: String? get() = auth.currentEmail

    /** Cambia el nombre de jugador (RLS solo deja editar la fila propia) y vuelve a sincronizar. */
    suspend fun updateDisplayName(name: String) {
        updatePlayer { put("display_name", name.trim()) }
        players.sync(waitIfBusy = true)
    }

    suspend fun setPresetAvatar(key: String) = saveAvatarUrl(presetAvatarValue(key))

    /** Sube la foto ya procesada (WEBP 512 × 512) reemplazando la anterior y la deja como avatar. */
    suspend fun uploadAvatar(webp: ByteArray) {
        val uid = requireUid()
        supabase.storage.from(AVATAR_BUCKET).upload(photoAvatarPath(uid), webp) {
            upsert = true
            contentType = WEBP
        }
        // La versión cambia en cada subida para que Coil no muestre la foto vieja.
        saveAvatarUrl(photoAvatarValue(uid, System.currentTimeMillis() / 1_000))
    }

    /** Quita la foto propia: vuelve al avatar por defecto. El archivo se sobrescribe en la próxima subida. */
    suspend fun clearAvatar() = saveAvatarUrl(null)

    private suspend fun saveAvatarUrl(value: String?) {
        updatePlayer { put("avatar_url", value) }
        cache.updateHome { home ->
            home.copy(profile = (home.profile ?: PlayerProfile(home.snapshot.player?.displayName)).copy(avatarUrl = value))
        }
    }

    private suspend fun updatePlayer(fields: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
        val uid = requireUid()
        supabase.postgrest.from("players").update(buildJsonObject(fields)) {
            filter { eq("id", uid) }
        }
    }

    private fun requireUid(): String = auth.currentUserId ?: error("No autenticado")

    private companion object {
        val WEBP = ContentType("image", "webp")
    }
}
