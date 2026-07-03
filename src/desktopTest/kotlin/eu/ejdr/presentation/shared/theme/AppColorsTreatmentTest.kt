package eu.ejdr.presentation.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppColorsTreatmentTest {
    @Test
    fun `grimoire expose un ornement laiton et un gradient d'accent`() {
        val c = grimoireColors()
        // l'ornement doré du grimoire = brass
        assertEquals(GrimoirePalette.brass, c.ornament)
        // le haut du gradient est plus clair que le bas (relief)
        assertEquals(GrimoirePalette.brassHi, c.accentGradientTop)
        assertEquals(GrimoirePalette.brass, c.accentGradientBottom)
    }

    @Test
    fun `les trois themes fournissent tous les nouveaux roles`() {
        listOf(parchmentColors(), taupeColors(), grimoireColors()).forEach { c ->
            assertTrue(c.ornament.alpha > 0f)
            assertTrue(c.hairline.alpha >= 0f)
            assertTrue(c.accentGradientTop.alpha > 0f)
            assertTrue(c.accentGradientBottom.alpha > 0f)
        }
    }
}
