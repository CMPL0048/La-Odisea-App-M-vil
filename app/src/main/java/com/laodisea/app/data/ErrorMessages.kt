package com.laodisea.app.data

import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

/** ¿El error vino de no tener red (y no de una respuesta del servidor)? */
fun Throwable.isNetworkError(): Boolean =
    generateSequence(this) { it.cause }.any {
        it is HttpRequestException || it is HttpRequestTimeoutException || it is IOException
    }

/** ¿Supabase no mandó el correo porque se pidió muy seguido? */
fun Throwable.isEmailRateLimit(): Boolean {
    val text = message.orEmpty()
    return text.contains("over_email_send_rate_limit", ignoreCase = true) ||
        text.contains("email rate limit", ignoreCase = true) ||
        text.contains("only request this after", ignoreCase = true)
}

/**
 * ¿El servidor respondió que la función no existe? (PostgREST PGRST202 o Postgres 42883.)
 * Sirve para seguir funcionando con un servidor que aún no tiene la migración de partidas.
 */
fun Throwable.isMissingFunction(): Boolean {
    val text = message.orEmpty()
    return text.contains("PGRST202") || text.contains("Could not find the function", ignoreCase = true) ||
        text.contains("42883") || (text.contains("function", ignoreCase = true) && text.contains("does not exist", ignoreCase = true))
}

/**
 * Todos los mensajes de las funciones de partidas vinculadas (migración 08). Ya vienen en español
 * y se muestran tal cual. «No autenticado» no está aquí: se trata como sesión vencida.
 */
private val SERVER_SAVE_MESSAGES = listOf(
    "El código no es válido o ya caducó",
    "Esta partida ya está vinculada a una cuenta",
    "Ya tienes 5 partidas. Desvincula una para agregar otra",
    "No puedes vincular tu propia cuenta",
    "Inicia sesión con tu cuenta para vincular partidas",
    "Esa partida no está vinculada a tu cuenta",
    "El nombre debe tener de 1 a 24 caracteres",
)

/** Traduce los errores de Supabase a un mensaje corto en español para el jugador. */
fun Throwable.toUserMessage(): String {
    if (isNetworkError()) return "Sin conexión. Revisa tu internet e intenta de nuevo."
    if (this is AvatarImageException) return message.orEmpty()
    val text = message.orEmpty()
    SERVER_SAVE_MESSAGES.firstOrNull { text.contains(it, ignoreCase = true) }?.let { return "$it." }
    return when {
        text.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos."
        text.contains("Email not confirmed", ignoreCase = true) -> "Confirma tu correo antes de iniciar sesión."
        text.contains("already registered", ignoreCase = true) -> "Ya existe una cuenta con ese correo."
        text.contains("Password should be", ignoreCase = true) -> "La contraseña debe tener al menos 6 caracteres."
        text.contains("valid email", ignoreCase = true) || text.contains("invalid format", ignoreCase = true) ->
            "Escribe un correo válido."
        // Recuperación de contraseña con código de 6 dígitos.
        text.contains("otp_expired", ignoreCase = true) || text.contains("expired or is invalid", ignoreCase = true) ->
            "El código no es válido o ya caducó. Pide uno nuevo."
        isEmailRateLimit() -> "Espera un momento antes de pedir otro código."
        text.contains("same_password", ignoreCase = true) -> "La nueva contraseña debe ser distinta a la anterior."
        text.contains("rate limit", ignoreCase = true) -> "Demasiados intentos. Espera un momento."
        // Storage rechaza archivos de más de 1 MB en el bucket de avatares.
        text.contains("maximum allowed size", ignoreCase = true) || text.contains("Payload too large", ignoreCase = true) ->
            "La foto es muy grande (máximo 1 MB)."
        text.contains("No autenticado", ignoreCase = true) || text.contains("JWT", ignoreCase = true) ->
            "Tu sesión expiró. Vuelve a iniciar sesión."
        // Mensajes que lanzan las funciones de minijuegos del servidor.
        text.contains("Sin intentos disponibles", ignoreCase = true) ->
            "Ya no te quedan intentos. Vuelve cuando se recarguen."
        text.contains("Monedas insuficientes", ignoreCase = true) -> "No te alcanzan las monedas."
        text.contains("Negociación no encontrada", ignoreCase = true) -> "Esa negociación ya terminó."
        text.contains("Oferta inválida", ignoreCase = true) -> "Escribe una oferta válida."
        text.contains("no tiene objetos en venta", ignoreCase = true) -> "Vicente no tiene nada que vender ahora."
        else -> "Algo salió mal. Intenta de nuevo."
    }
}
