package eu.ejdr.presentation.features.charactersheet.component

import eu.ejdr.domain.features.charactersheet.entities.ResolvedFormation
import eu.ejdr.domain.features.charactersheet.entities.ResolvedReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatDisplayTest {

    private fun peuple(stat: String?, bonus: Int?) =
        ResolvedReference(id = "p-1", name = "Elfe", stat = stat, bonus = bonus)

    private fun formation(stat: String?, bonus: Int?) =
        ResolvedFormation(id = "f-1", name = "Rôdeur", stat = stat, bonus = bonus)

    @Test
    fun `no source means no bonus and total equals base`() {
        val display = statDisplay("social", base = 2, formation = null, peuple = null)

        assertEquals(2, display.base)
        assertTrue(display.bonuses.isEmpty())
        assertEquals(2, display.total)
        assertFalse(display.hasBonus)
    }

    @Test
    fun `null base with no source totals zero`() {
        val display = statDisplay("social", base = null, formation = null, peuple = null)

        assertTrue(display.bonuses.isEmpty())
        assertEquals(0, display.total)
    }

    @Test
    fun `peuple targeting the stat adds a single bonus`() {
        val display = statDisplay("social", base = 2, formation = null, peuple = peuple("social", 1))

        assertEquals(listOf(1), display.bonuses)
        assertEquals(3, display.total)
        assertTrue(display.hasBonus)
    }

    @Test
    fun `peuple then formation both targeting the stat stack in order`() {
        val display = statDisplay(
            "social",
            base = 2,
            formation = formation("social", 2),
            peuple = peuple("social", 1),
        )

        assertEquals(listOf(1, 2), display.bonuses)
        assertEquals(5, display.total)
    }

    @Test
    fun `formation targeting another stat does not affect this stat`() {
        val display = statDisplay(
            "social",
            base = 2,
            formation = formation("vigueur", 2),
            peuple = null,
        )

        assertTrue(display.bonuses.isEmpty())
        assertEquals(2, display.total)
    }

    @Test
    fun `source targeting the stat but with null bonus contributes nothing`() {
        val display = statDisplay("social", base = 2, formation = null, peuple = peuple("social", null))

        assertTrue(display.bonuses.isEmpty())
        assertEquals(2, display.total)
    }

    @Test
    fun `only the source targeting the stat contributes when both exist`() {
        val display = statDisplay(
            "social",
            base = 2,
            formation = formation("social", 2),
            peuple = peuple("vigueur", 1),
        )

        assertEquals(listOf(2), display.bonuses)
        assertEquals(4, display.total)
    }
}
