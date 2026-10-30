package com.laodisea.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveLinkTest {

    @Test
    fun `lee el QR del juego`() {
        assertEquals(LinkCodeResult.Valid("ABCD2345"), parseLinkCode("laodisea://vincular?c=ABCD2345"))
        assertEquals(LinkCodeResult.Valid("ABCD2345"), parseLinkCode("LAODISEA://Vincular/?c=abcd2345"))
        assertEquals(LinkCodeResult.Valid("ABCD2345"), parseLinkCode("laodisea://vincular?v=1&c=ABCD-2345"))
    }

    @Test
    fun `acepta solo el codigo y lo normaliza`() {
        assertEquals(LinkCodeResult.Valid("ABCD2345"), parseLinkCode("abcd2345"))
        assertEquals(LinkCodeResult.Valid("ABCD2345"), parseLinkCode("  ABCD-2345 "))
        assertEquals(LinkCodeResult.Valid("ABCD2345"), parseLinkCode("abcd 2345"))
    }

    @Test
    fun `rechaza codigos con largo o letras invalidas`() {
        assertTrue(parseLinkCode("ABC2345") is LinkCodeResult.Invalid)
        assertTrue(parseLinkCode("ABCD23456") is LinkCodeResult.Invalid)
        // O, I, 0 y 1 no existen en el alfabeto del código.
        assertTrue(parseLinkCode("ABCD2340") is LinkCodeResult.Invalid)
        assertTrue(parseLinkCode("OBCD2345") is LinkCodeResult.Invalid)
        assertTrue(parseLinkCode("") is LinkCodeResult.Invalid)
    }

    @Test
    fun `rechaza QR que no son de La Odisea`() {
        val other = parseLinkCode("https://example.com/?c=ABCD2345")
        assertEquals(LinkCodeResult.Invalid("Ese QR no es de La Odisea. Escanea el que aparece en el juego."), other)
        assertTrue(parseLinkCode("laodisea://otra?c=ABCD2345") is LinkCodeResult.Invalid)
        assertTrue(parseLinkCode("laodisea://vincular") is LinkCodeResult.Invalid)
    }

    @Test
    fun `nombre de partida de 1 a 24 caracteres`() {
        assertEquals("Escribe un nombre para la partida.", saveNameError("   "))
        assertEquals("El nombre puede tener hasta 24 caracteres.", saveNameError("x".repeat(25)))
        assertNull(saveNameError("PC de la casa"))
    }
}
