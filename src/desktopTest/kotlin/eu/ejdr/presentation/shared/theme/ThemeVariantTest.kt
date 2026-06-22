package eu.ejdr.presentation.shared.theme

import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeVariantTest {
    @Test
    fun `colorsFor mappe chaque variante vers sa palette`() {
        assertEquals(parchmentColors(), colorsFor(ThemeVariant.PARCHEMIN))
        assertEquals(taupeColors(), colorsFor(ThemeVariant.TAUPE))
        assertEquals(grimoireColors(), colorsFor(ThemeVariant.GRIMOIRE))
    }

    @Test
    fun `le defaut est parchemin`() {
        assertEquals(ThemeVariant.PARCHEMIN, ThemeVariant.DEFAULT)
    }
}
