package eu.ejdr.presentation.shared.feedback

import kotlin.test.Test
import kotlin.test.assertEquals

class UiMessageTest {
    @Test
    fun `success produit le ton SUCCESS`() {
        val m = UiMessage.success("Campagne créée")
        assertEquals("Campagne créée", m.text)
        assertEquals(UiMessageTone.SUCCESS, m.tone)
    }

    @Test
    fun `error produit le ton ERROR`() {
        val m = UiMessage.error("Échec réseau")
        assertEquals(UiMessageTone.ERROR, m.tone)
    }
}
