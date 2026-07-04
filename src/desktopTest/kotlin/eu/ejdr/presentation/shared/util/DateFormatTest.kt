package eu.ejdr.presentation.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateFormatTest {
    @Test
    fun `formatDate rend un jour lisible en francais`() {
        assertEquals("22 juin 2026", formatDate("2026-06-22T10:30:00Z"))
        assertEquals("1 janvier 2026", formatDate("2026-01-01"))
    }

    @Test
    fun `formatDate tolere une entree invalide en la renvoyant brute`() {
        assertEquals("pas-une-date", formatDate("pas-une-date"))
        assertEquals("", formatDate(""))
    }

    @Test
    fun `relativeDate calcule aujourd'hui, futur et passe`() {
        assertEquals("aujourd'hui", relativeDate("2026-06-22", todayIso = "2026-06-22"))
        assertEquals("dans 3 jours", relativeDate("2026-06-25", todayIso = "2026-06-22"))
        assertEquals("il y a 2 jours", relativeDate("2026-06-20", todayIso = "2026-06-22"))
        assertEquals("demain", relativeDate("2026-06-23", todayIso = "2026-06-22"))
        assertEquals("hier", relativeDate("2026-06-21", todayIso = "2026-06-22"))
    }

    @Test
    fun `relativeDate renvoie null hors fenetre pertinente ou si invalide`() {
        assertNull(relativeDate("2026-08-01", todayIso = "2026-06-22")) // > 7 jours
        assertNull(relativeDate("invalide", todayIso = "2026-06-22"))
    }

    @Test
    fun `relativeDate franchit correctement un changement d'annee`() {
        assertEquals("demain", relativeDate("2027-01-01", todayIso = "2026-12-31"))
        assertEquals("hier", relativeDate("2026-12-31", todayIso = "2027-01-01"))
    }

    @Test
    fun `relativeDate renvoie null si todayIso est invalide`() {
        assertNull(relativeDate("2026-06-22", todayIso = "pas-une-date"))
    }
}
