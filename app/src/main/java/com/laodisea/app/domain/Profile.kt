package com.laodisea.app.domain

/** Bucket público de Supabase Storage con las fotos de perfil. */
const val AVATAR_BUCKET = "avatars"

const val DISPLAY_NAME_MIN_LENGTH = 3
const val DISPLAY_NAME_MAX_LENGTH = 24

/**
 * Lo que guarda `public.players.avatar_url`. El servidor solo acepta (CHECK) tres formas:
 * null, `preset:<clave>` o `<uid>/avatar.webp?v=<número>`.
 */
sealed interface AvatarRef {
    /** Sin avatar elegido: se muestra el de Víctor. */
    data object Default : AvatarRef

    /** Uno de los avatares precargados de la app. */
    data class Preset(val key: String) : AvatarRef

    /**
     * Foto propia en el bucket. [path] es `<uid>/avatar.webp`; [version] cambia en cada subida
     * para que la caché de imágenes no muestre la foto anterior.
     */
    data class Photo(val path: String, val version: Long) : AvatarRef
}

private const val PRESET_PREFIX = "preset:"
private const val PHOTO_FILE = "avatar.webp"
private val PRESET_KEY = Regex("^[a-z0-9_]{1,32}$")
private val PHOTO_VALUE = Regex("^([0-9a-fA-F-]{36})/avatar\\.webp\\?v=(\\d{1,18})$")

/** Interpreta `avatar_url`. Cualquier valor que no reconozca se trata como sin avatar. */
fun parseAvatarUrl(raw: String?): AvatarRef {
    if (raw.isNullOrBlank()) return AvatarRef.Default
    if (raw.startsWith(PRESET_PREFIX)) {
        val key = raw.removePrefix(PRESET_PREFIX)
        return if (PRESET_KEY.matches(key)) AvatarRef.Preset(key) else AvatarRef.Default
    }
    val match = PHOTO_VALUE.matchEntire(raw) ?: return AvatarRef.Default
    val (uid, version) = match.destructured
    return AvatarRef.Photo(photoAvatarPath(uid), version.toLong())
}

fun presetAvatarValue(key: String): String {
    require(PRESET_KEY.matches(key)) { "Clave de avatar inválida: $key" }
    return PRESET_PREFIX + key
}

/** Ruta del archivo dentro del bucket; siempre la misma para que cada subida reemplace la anterior. */
fun photoAvatarPath(uid: String): String = "$uid/$PHOTO_FILE"

fun photoAvatarValue(uid: String, version: Long): String = "${photoAvatarPath(uid)}?v=$version"

/**
 * URL pública de la foto, con el mismo formato que `storage.from(bucket).publicUrl(path)` de
 * supabase-kt, más `?v=` para invalidar la caché de Coil cuando cambia.
 */
fun avatarPublicUrl(supabaseUrl: String, photo: AvatarRef.Photo): String =
    "${supabaseUrl.trimEnd('/')}/storage/v1/object/public/$AVATAR_BUCKET/${photo.path}?v=${photo.version}"

/** Recorta lo que se escribe en el campo del nombre al máximo permitido. */
fun limitDisplayName(value: String): String = value.take(DISPLAY_NAME_MAX_LENGTH)

/** Error del nombre de jugador, o null si es válido. */
fun displayNameError(name: String): String? =
    if (name.trim().length < DISPLAY_NAME_MIN_LENGTH) {
        "El nombre de jugador debe tener al menos $DISPLAY_NAME_MIN_LENGTH letras."
    } else {
        null
    }
