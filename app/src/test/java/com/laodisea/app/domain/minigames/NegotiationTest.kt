package com.laodisea.app.domain.minigames

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NegotiationTest {

    // Así la abre el servidor para el Chaleco (precio 200): pide el doble, paciencia 100, 3 turnos.
    private val opened = Negotiation(id = 7, itemCode = "CHALECO", askingPrice = 400, patience = 100, turnsLeft = 3)

    @Test
    fun validatesOffersBeforeSending() {
        assertEquals(OfferError.Vacia, opened.validateOffer(null, coins = 120))
        assertEquals(OfferError.Vacia, opened.validateOffer(0, coins = 120))
        assertEquals(OfferError.SinMonedas(120), opened.validateOffer(150, coins = 120))
        assertEquals(OfferError.MayorQuePrecio(400), opened.validateOffer(450, coins = 1_000))
        assertNull(opened.validateOffer(100, coins = 120))
    }

    @Test
    fun counterOfferUpdatesPriceAndPatience() {
        val next = opened.apply(100, OfferResponse.Contraoferta(askingPrice = 316, patience = 75, turnsLeft = 2))
        assertTrue(next.isOpen)
        assertEquals(316, next.askingPrice)
        assertEquals(75, next.patience)
        assertEquals(2, next.turnsLeft)
        assertEquals(Mood.Tranquilo, next.mood)
        assertEquals(listOf(Negotiation.Exchange(100, Negotiation.Reply.Contraoferta, 316)), next.history)
    }

    @Test
    fun acceptedClosesWithFinalPrice() {
        val closed = opened.apply(130, OfferResponse.Aceptada("CHALECO", 130))
        assertEquals(Negotiation.Status.Aceptada, closed.status)
        assertEquals(130L, closed.finalPrice)
        assertFalse(closed.isOpen)
        assertEquals(OfferError.Cerrada, closed.validateOffer(10, coins = 120))
    }

    @Test
    fun walkAwayClosesWithoutPrice() {
        val gone = opened.apply(10, OfferResponse.SeRetiro)
        assertEquals(Negotiation.Status.SeRetiro, gone.status)
        assertNull(gone.finalPrice)
        assertEquals(0, gone.patience)
        assertEquals(Mood.AlLimite, gone.mood)
    }

    @Test
    fun moodFollowsPatience() {
        assertEquals(Mood.Tranquilo, Mood.of(100))
        assertEquals(Mood.Impaciente, Mood.of(50))
        assertEquals(Mood.Molesto, Mood.of(25))
        assertEquals(Mood.AlLimite, Mood.of(0))
    }

    @Test
    fun suggestionsAreAffordableAndBelowAskingPrice() {
        assertEquals(listOf(160L, 240L, 320L), opened.suggestedOffers(coins = 1_000))
        // Con 120 monedas no se sugiere nada que no pueda pagar.
        assertEquals(emptyList<Long>(), opened.suggestedOffers(coins = 120))
        assertTrue(opened.suggestedOffers(coins = 1_000).all { opened.validateOffer(it, 1_000) == null })
    }
}
