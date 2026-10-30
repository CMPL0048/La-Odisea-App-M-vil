package com.laodisea.app.domain

/** Una cuenta ve su partida principal más hasta 4 vinculadas. */
const val MAX_SAVES = 5

const val SAVE_NAME_MAX_LENGTH = 24

/** Largo del código que muestra el juego de PC para vincular una partida. */
const val LINK_CODE_LENGTH = 8

/** Letras y números del código: sin O, I, 0 ni 1 para que no se confundan al escribirlo. */
const val LINK_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

private const val LINK_SCHEME = "laodisea://"
private const val LINK_HOST = "vincular"

/** Resultado de leer un QR o un código escrito a mano. */
sealed interface LinkCodeResult {
    data class Valid(val code: String) : LinkCodeResult
    data class Invalid(val message: String) : LinkCodeResult
}

/** Mayúsculas y sin espacios ni guiones, como espera el servidor. */
fun normalizeLinkCode(input: String): String =
    input.uppercase().filterNot { it.isWhitespace() || it == '-' }

/**
 * Lee el contenido del QR del juego (`laodisea://vincular?c=CODIGO`) o un código suelto
 * y lo deja listo para `app.link_save`.
 */
fun parseLinkCode(raw: String): LinkCodeResult {
    val text = raw.trim()
    if (text.isEmpty()) return LinkCodeResult.Invalid("Escribe el código que aparece en el juego.")
    val candidate = when {
        text.startsWith(LINK_SCHEME, ignoreCase = true) -> {
            val rest = text.substring(LINK_SCHEME.length)
            val host = rest.substringBefore('?').trimEnd('/')
            if (!host.equals(LINK_HOST, ignoreCase = true)) return notOdiseaQr()
            rest.substringAfter('?', "")
                .split('&')
                .firstOrNull { it.startsWith("c=", ignoreCase = true) }
                ?.substring(2)
                ?: return notOdiseaQr()
        }
        // Cualquier otro enlace (http, otra app…) no es un QR de vinculación.
        "://" in text -> return notOdiseaQr()
        else -> text
    }
    val code = normalizeLinkCode(candidate)
    return if (isValidLinkCode(code)) LinkCodeResult.Valid(code) else LinkCodeResult.Invalid(INVALID_CODE)
}

fun isValidLinkCode(code: String): Boolean =
    code.length == LINK_CODE_LENGTH && code.all { it in LINK_CODE_ALPHABET }

/** Error del nombre de una partida vinculada (1–24 caracteres), o null si es válido. */
fun saveNameError(name: String): String? = when {
    name.isBlank() -> "Escribe un nombre para la partida."
    name.trim().length > SAVE_NAME_MAX_LENGTH -> "El nombre puede tener hasta $SAVE_NAME_MAX_LENGTH caracteres."
    else -> null
}

private const val INVALID_CODE = "El código tiene 8 letras y números (sin O, I, 0 ni 1). Revísalo en el juego."

private fun notOdiseaQr() = LinkCodeResult.Invalid("Ese QR no es de La Odisea. Escanea el que aparece en el juego.")
