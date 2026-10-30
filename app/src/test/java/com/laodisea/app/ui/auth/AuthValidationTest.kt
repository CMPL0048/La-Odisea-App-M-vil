package com.laodisea.app.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `el codigo solo acepta digitos y como maximo 6`() {
        assertEquals("123456", sanitizeResetCode("123456789"))
        assertEquals("1234", sanitizeResetCode("12 3-4"))
        assertEquals("", sanitizeResetCode("abc"))
        // Dígitos de otros alfabetos no cuentan: Supabase solo manda 0-9.
        assertEquals("", sanitizeResetCode("١٢٣"))
    }

    @Test
    fun `el codigo valido tiene exactamente 6 digitos`() {
        assertTrue(isValidResetCode("012345"))
        assertFalse(isValidResetCode("12345"))
        assertFalse(isValidResetCode("1234567"))
        assertFalse(isValidResetCode("12a456"))
        assertFalse(isValidResetCode(""))
    }

    @Test
    fun `la contrasena nueva pide minimo 6 caracteres y que coincida`() {
        assertEquals("La contraseña debe tener al menos 6 caracteres.", newPasswordError("12345", "12345"))
        assertEquals("Las contraseñas no coinciden.", newPasswordError("secreto1", "secreto2"))
        assertNull(newPasswordError("secreto", "secreto"))
    }
}
