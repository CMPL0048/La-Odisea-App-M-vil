package com.laodisea.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileTest {

    private val uid = "0f8c2b9e-1d3a-4c5b-9e7f-a1b2c3d4e5f6"

    @Test
    fun `sin avatar o con valores desconocidos se usa el de Victor`() {
        assertEquals(AvatarRef.Default, parseAvatarUrl(null))
        assertEquals(AvatarRef.Default, parseAvatarUrl(""))
        assertEquals(AvatarRef.Default, parseAvatarUrl("https://otro.sitio/foto.png"))
        assertEquals(AvatarRef.Default, parseAvatarUrl("preset:"))
        assertEquals(AvatarRef.Default, parseAvatarUrl("preset:Con-Mayusculas"))
        assertEquals(AvatarRef.Default, parseAvatarUrl("$uid/avatar.webp"))
        assertEquals(AvatarRef.Default, parseAvatarUrl("$uid/otra.webp?v=1"))
    }

    @Test
    fun `lee avatares precargados`() {
        assertEquals(AvatarRef.Preset("salva"), parseAvatarUrl("preset:salva"))
        assertEquals(AvatarRef.Preset("tono_2"), parseAvatarUrl("preset:tono_2"))
    }

    @Test
    fun `lee la foto propia con su version`() {
        assertEquals(AvatarRef.Photo("$uid/avatar.webp", 1730300000), parseAvatarUrl("$uid/avatar.webp?v=1730300000"))
    }

    @Test
    fun `los valores que se guardan cumplen el formato del servidor y se pueden volver a leer`() {
        assertEquals("preset:juanma", presetAvatarValue("juanma"))
        assertEquals(AvatarRef.Preset("juanma"), parseAvatarUrl(presetAvatarValue("juanma")))

        val value = photoAvatarValue(uid, 42)
        assertEquals("$uid/avatar.webp?v=42", value)
        assertEquals(AvatarRef.Photo(photoAvatarPath(uid), 42), parseAvatarUrl(value))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no deja guardar claves de avatar invalidas`() {
        presetAvatarValue("no valida")
    }

    @Test
    fun `la url publica apunta al bucket de avatares con la version`() {
        assertEquals(
            "https://abc.supabase.co/storage/v1/object/public/avatars/$uid/avatar.webp?v=7",
            avatarPublicUrl("https://abc.supabase.co/", AvatarRef.Photo("$uid/avatar.webp", 7)),
        )
    }

    @Test
    fun `el nombre de jugador pide al menos 3 letras sin contar espacios`() {
        assertEquals("El nombre de jugador debe tener al menos 3 letras.", displayNameError("Ab"))
        assertEquals("El nombre de jugador debe tener al menos 3 letras.", displayNameError("  Ab  "))
        assertNull(displayNameError("Víc"))
    }

    @Test
    fun `el nombre de jugador se corta en 24 caracteres`() {
        assertEquals(24, limitDisplayName("x".repeat(40)).length)
        assertEquals("Víctor", limitDisplayName("Víctor"))
    }
}
