package com.laodisea.app.ui.auth

/** Largo del código que manda Supabase para recuperar la contraseña. */
const val RESET_CODE_LENGTH = 6

/** Largo mínimo de contraseña, igual que en el registro y en Supabase. */
const val MIN_PASSWORD_LENGTH = 6

/** Deja solo dígitos y como máximo [RESET_CODE_LENGTH], para lo que se escribe o se pega en el campo. */
fun sanitizeResetCode(input: String): String = input.filter { it in '0'..'9' }.take(RESET_CODE_LENGTH)

fun isValidResetCode(code: String): Boolean = code.length == RESET_CODE_LENGTH && code.all { it in '0'..'9' }

/** Error de la contraseña nueva y su confirmación, o null si están bien. */
fun newPasswordError(password: String, confirmPassword: String): String? = when {
    password.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres."
    password != confirmPassword -> "Las contraseñas no coinciden."
    else -> null
}
