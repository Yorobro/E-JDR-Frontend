package eu.ejdr.presentation.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppMotionTest {
    @Test
    fun `valeurs par defaut sobres`() {
        val m = AppMotion()
        assertEquals(120, m.durationFast)
        assertEquals(200, m.durationMedium)
        assertEquals(0.97f, m.pressScale)
        assertTrue(m.enabled)
    }

    @Test
    fun `desactiver le mouvement ramene les durees effectives a zero`() {
        val m = AppMotion(enabled = false)
        assertEquals(0, m.effectiveDuration(m.durationFast))
        assertEquals(0, m.effectiveDuration(m.durationMedium))
    }

    @Test
    fun `mouvement actif conserve les durees`() {
        val m = AppMotion(enabled = true)
        assertEquals(120, m.effectiveDuration(m.durationFast))
    }
}
