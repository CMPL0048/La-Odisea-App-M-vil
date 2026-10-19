package com.laodisea.app.data

import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

/** ¿El error vino de no tener red (y no de una respuesta del servidor)? */
fun Throwable.isNetworkError(): Boolean =
    generateSequence(this) { it.cause }.any {
        it is HttpRequestException || it is HttpRequestTimeoutException || it is IOException
    }

/** Traduce los errores de Supabase a un mensaje corto en español para el jugador. */
fun Throwable.toUserMessage(): String {
    if (isNetworkError()) return "Sin conexión. Revisa tu internet e intenta de nuevo."
    val text = message.orEmpty()
    return when {
        text.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos."
        text.contains("Email not confirmed", ignoreCase = true) -> "Confirma tu correo antes de iniciar sesión."
        text.contains("already registered", ignoreCase = true) -> "Ya existe una cuenta con ese correo."
        text.contains("Password should be", ignoreCase = true) -> "La contraseña debe tener al menos 6 caracteres."
        text.contains("valid email", ignoreCase = true) || text.contains("invalid format", ignoreCase = true) ->
            "Escribe un correo válido."
        text.contains("rate limit", ignoreCase = true) -> "Demasiados intentos. Espera un momento."
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
