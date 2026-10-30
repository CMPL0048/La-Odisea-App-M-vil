package com.laodisea.app.domain

/** Lado en píxeles de la foto de perfil que se sube (cuadrada). */
const val AVATAR_SIZE_PX = 512

/** Límite del bucket `avatars`: 1 MB. */
const val AVATAR_MAX_BYTES = 1024 * 1024

/** Calidad WEBP con la que se comprime la foto. */
const val AVATAR_WEBP_QUALITY = 85

/** Cuadrado dentro de la imagen original: esquina superior izquierda y lado. */
data class SquareCrop(val left: Int, val top: Int, val size: Int)

/** El cuadrado más grande centrado en una imagen de [width] × [height]. */
fun centerSquareCrop(width: Int, height: Int): SquareCrop {
    require(width > 0 && height > 0) { "Imagen vacía" }
    val size = minOf(width, height)
    return SquareCrop(left = (width - size) / 2, top = (height - size) / 2, size = size)
}

/**
 * Mayor potencia de 2 para submuestrear al decodificar sin que el lado corto quede por debajo de
 * [target]. Evita cargar en memoria fotos de 12 MP completas solo para hacer un avatar.
 */
fun sampleSizeFor(width: Int, height: Int, target: Int = AVATAR_SIZE_PX): Int {
    val shortSide = minOf(width, height)
    var sample = 1
    while (shortSide / (sample * 2) >= target) sample *= 2
    return sample
}
