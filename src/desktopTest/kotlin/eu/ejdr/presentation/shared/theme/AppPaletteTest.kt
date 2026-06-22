package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertNotEquals

class AppPaletteTest {
    @Test
    fun `les accents signature des trois themes sont distincts`() {
        assertNotEquals(ParchmentPalette.bole, GrimoirePalette.brass)
        assertNotEquals(TaupePalette.taupe, ParchmentPalette.bole)
    }

    @Test
    fun `aucune teinte n'est transparente`() {
        val all = listOf(
            ParchmentPalette.parchment, ParchmentPalette.vellum, ParchmentPalette.ink, ParchmentPalette.bole,
            TaupePalette.background, TaupePalette.surface, TaupePalette.taupe, TaupePalette.ink,
            GrimoirePalette.background, GrimoirePalette.surface, GrimoirePalette.brass, GrimoirePalette.cream,
        )
        all.forEach { assertNotEquals(0f, it.alpha) }
    }
}
