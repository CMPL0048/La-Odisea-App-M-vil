package com.laodisea.app.data

import com.laodisea.app.ui.auth.blocksCodeSending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ErrorMessagesTest {

    @Test
    fun `detecta bloqueo de correo no autorizado en plan gratuito de Supabase`() {
        val unauthorized = RuntimeException("Email address \"jugador@ejemplo.com\" cannot be used as it is not authorized (email_address_not_authorized)")
        assertTrue(unauthorized.isEmailDeliveryBlocked())
        assertTrue(unauthorized.blocksCodeSending())
        assertEquals(
            "Supabase no autoriza envíos a este correo en el plan gratuito (agrégalo al equipo o configura SMTP).",
            unauthorized.toUserMessage(),
        )
    }

    @Test
    fun `detecta fallo de servidor SMTP y limite de envios por hora`() {
        val smtpError = RuntimeException("Error sending recovery email")
        assertTrue(smtpError.isEmailDeliveryBlocked())
        assertTrue(smtpError.blocksCodeSending())
        assertEquals(
            "No se pudo enviar el correo de recuperación. Revisa la configuración de correo en Supabase.",
            smtpError.toUserMessage(),
        )

        val rateLimit = RuntimeException("over_email_send_rate_limit: email rate limit exceeded")
        assertTrue(rateLimit.isEmailRateLimit())
        assertTrue(rateLimit.blocksCodeSending())
        assertEquals(
            "Espera un momento antes de pedir otro código.",
            rateLimit.toUserMessage(),
        )
    }

    @Test
    fun `errores de red bloquean el envio pero errores de existencia de cuenta no se revelan`() {
        val offline = IOException("Unable to resolve host")
        assertTrue(offline.blocksCodeSending())
        assertEquals("Sin conexión. Revisa tu internet e intenta de nuevo.", offline.toUserMessage())

        val userNotFound = RuntimeException("User not found")
        assertFalse(userNotFound.blocksCodeSending())
    }

    @Test
    fun `traduce errores del codigo OTP y cambio de contrasena`() {
        val expired = RuntimeException("Token has expired or is invalid (otp_expired)")
        assertEquals("El código no es válido o ya caducó. Pide uno nuevo.", expired.toUserMessage())

        val samePassword = RuntimeException("New password should be different from the old password (same_password)")
        assertEquals("La nueva contraseña debe ser distinta a la anterior.", samePassword.toUserMessage())

        val shortPassword = RuntimeException("Password should be at least 6 characters.")
        assertEquals("La contraseña debe tener al menos 6 caracteres.", shortPassword.toUserMessage())
    }
}
