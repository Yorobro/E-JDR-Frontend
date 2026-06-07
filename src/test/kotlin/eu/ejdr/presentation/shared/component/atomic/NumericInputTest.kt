package eu.ejdr.presentation.shared.component.atomic

import kotlin.test.Test
import kotlin.test.assertEquals

class NumericInputTest {

    @Test
    fun `keeps digits only by default`() {
        assertEquals("123", filterNumericInput("1a2b3", allowDecimal = false, allowNegative = false))
    }

    @Test
    fun `empty string stays empty`() {
        assertEquals("", filterNumericInput("", allowDecimal = false, allowNegative = false))
    }

    @Test
    fun `decimal allowed keeps a single dot`() {
        assertEquals("12.34", filterNumericInput("12.34", allowDecimal = true, allowNegative = false))
    }

    @Test
    fun `decimal allowed drops extra dots`() {
        // Règle : garder tous les chiffres + le premier point uniquement ; les points
        // suivants sont supprimés mais les chiffres restants sont conservés.
        assertEquals("1.234", filterNumericInput("1.2.3.4", allowDecimal = true, allowNegative = false))
    }

    @Test
    fun `negative allowed keeps leading minus only`() {
        assertEquals("-12", filterNumericInput("-1-2", allowDecimal = false, allowNegative = true))
    }

    @Test
    fun `negative not allowed strips minus`() {
        assertEquals("12", filterNumericInput("-12", allowDecimal = false, allowNegative = false))
    }
}
