package com.laodisea.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarImageTest {

    @Test
    fun `recorta el cuadrado central de fotos horizontales y verticales`() {
        assertEquals(SquareCrop(left = 500, top = 0, size = 3000), centerSquareCrop(4000, 3000))
        assertEquals(SquareCrop(left = 0, top = 500, size = 3000), centerSquareCrop(3000, 4000))
        assertEquals(SquareCrop(left = 0, top = 0, size = 800), centerSquareCrop(800, 800))
        // Con lado impar el sobrante se redondea hacia arriba-izquierda.
        assertEquals(SquareCrop(left = 0, top = 1, size = 3), centerSquareCrop(3, 6))
    }

    @Test
    fun `submuestrea sin bajar de 512 en el lado corto`() {
        assertEquals(4, sampleSizeFor(4000, 3000)) // 3000 / 4 = 750
        assertEquals(1, sampleSizeFor(1000, 900))
        assertEquals(2, sampleSizeFor(1024, 1024)) // justo 512
        assertEquals(1, sampleSizeFor(300, 200)) // fotos pequeñas no se reducen
    }
}
