package eu.ejdr.presentation.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppColorsTest {
    @Test
    fun `parchemin et taupe sont des themes clairs, grimoire sombre`() {
        assertFalse(parchmentColors().isDark)
        assertFalse(taupeColors().isDark)
        assertTrue(grimoireColors().isDark)
    }

    @Test
    fun `chaque role pointe vers la teinte brute attendue`() {
        assertEquals(ParchmentPalette.bole, parchmentColors().primary)
        assertEquals(ParchmentPalette.parchment, parchmentColors().background)
        assertEquals(TaupePalette.taupe, taupeColors().primary)
        assertEquals(GrimoirePalette.brass, grimoireColors().primary)
        assertEquals(GrimoirePalette.background, grimoireColors().background)
    }
}
