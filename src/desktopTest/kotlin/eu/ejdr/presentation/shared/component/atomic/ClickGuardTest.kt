package eu.ejdr.presentation.shared.component.atomic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClickGuardTest {
    @Test
    fun `premier clic accepte`() {
        assertTrue(ClickGuard(windowMs = 400L).tryClick(nowMs = 1000L))
    }

    @Test
    fun `clic rapproche rejete`() {
        val g = ClickGuard(windowMs = 400L)
        assertTrue(g.tryClick(1000L))
        assertFalse(g.tryClick(1200L)) // 200ms < 400ms
    }

    @Test
    fun `clic apres la fenetre accepte`() {
        val g = ClickGuard(windowMs = 400L)
        assertTrue(g.tryClick(1000L))
        assertTrue(g.tryClick(1500L)) // 500ms > 400ms
    }
}
