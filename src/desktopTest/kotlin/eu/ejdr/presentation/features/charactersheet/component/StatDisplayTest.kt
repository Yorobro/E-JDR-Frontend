package eu.ejdr.presentation.features.charactersheet.component

import eu.ejdr.domain.features.charactersheet.entities.ResolvedFormation
import eu.ejdr.domain.features.charactersheet.entities.ResolvedReference
import eu.ejdr.domain.features.reference.entities.ReferenceStatBonus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatDisplayTest {

    /**
     * Un peuple porte 0..N bonus (au plus un par stat). `peuple()` sans argument = aucun bonus ;
     * la formation, elle, reste mono-bonus.
     */
    private fun peuple(vararg bonuses: Pair<String, Int>) =
        ResolvedReference(
            id = "p-1",
            name = "Elfe",
            statBonuses = bonuses.map { ReferenceStatBonus(it.first, it.second) },
        )

    private fun formation(stat: String?, bonus: Int?) =
        ResolvedFormation(id = "f-1", name = "Rôdeur", stat = stat, bonus = bonus)

    @Test
    fun `no source means no bonus and total equals base`() {
        val display = statDisplay("social", base = 2, total = 2, formation = null, peuple = null)

        assertEquals(2, display.base)
        assertTrue(display.bonuses.isEmpty())
        assertEquals(2, display.total)
        assertFalse(display.hasBonus)
    }

    @Test
    fun `null base with no source falls back to total param`() {
        val display = statDisplay("social", base = null, total = 0, formation = null, peuple = null)

        assertTrue(display.bonuses.isEmpty())
        assertEquals(0, display.total)
    }

    @Test
    fun `peuple targeting the stat adds a single sourced bonus`() {
        val display =
            statDisplay("social", base = 2, total = 3, formation = null, peuple = peuple("social" to 1))

        assertEquals(listOf(StatBonus(StatBonusSource.PEUPLE, 1)), display.bonuses)
        assertEquals(3, display.total)
        assertTrue(display.hasBonus)
    }

    @Test
    fun `peuple then formation both targeting the stat stack in order`() {
        val display = statDisplay(
            "social",
            base = 2,
            total = 5,
            formation = formation("social", 2),
            peuple = peuple("social" to 1),
        )

        assertEquals(
            listOf(
                StatBonus(StatBonusSource.PEUPLE, 1),
                StatBonus(StatBonusSource.FORMATION, 2),
            ),
            display.bonuses,
        )
        assertEquals(5, display.total)
    }

    @Test
    fun `formation targeting another stat does not affect this stat`() {
        val display = statDisplay(
            "social",
            base = 2,
            total = 2,
            formation = formation("vigueur", 2),
            peuple = null,
        )

        assertTrue(display.bonuses.isEmpty())
        assertEquals(2, display.total)
    }

    @Test
    fun `a peuple without any stat bonus contributes nothing`() {
        val display = statDisplay("social", base = 2, total = 2, formation = null, peuple = peuple())

        assertTrue(display.bonuses.isEmpty())
        assertEquals(2, display.total)
    }

    @Test
    fun `a zero bonus contributes nothing`() {
        val display =
            statDisplay("social", base = 2, total = 2, formation = null, peuple = peuple("social" to 0))

        assertTrue(display.bonuses.isEmpty())
        assertEquals(2, display.total)
    }

    /**
     * Le cœur du correctif : un peuple porte plusieurs bonus, sur des stats différentes. Chacun ne
     * doit alimenter QUE la cellule de sa statistique.
     */
    @Test
    fun `a peuple with bonuses on two stats feeds each stat cell separately`() {
        val nain = peuple("vigueur" to 2, "social" to 1)

        val social = statDisplay("social", base = 1, total = 2, formation = null, peuple = nain)
        val vigueur = statDisplay("vigueur", base = 4, total = 6, formation = null, peuple = nain)
        val dexterite = statDisplay("dexterite", base = 3, total = 3, formation = null, peuple = nain)

        assertEquals(listOf(StatBonus(StatBonusSource.PEUPLE, 1)), social.bonuses)
        assertEquals(2, social.total)
        assertEquals(listOf(StatBonus(StatBonusSource.PEUPLE, 2)), vigueur.bonuses)
        assertEquals(6, vigueur.total)
        // Aucun bonus ne cible la dextérité.
        assertTrue(dexterite.bonuses.isEmpty())
        assertEquals(3, dexterite.total)
    }

    @Test
    fun `only the source targeting the stat contributes when both exist`() {
        val display = statDisplay(
            "social",
            base = 2,
            total = 4,
            formation = formation("social", 2),
            peuple = peuple("vigueur" to 1),
        )

        assertEquals(listOf(StatBonus(StatBonusSource.FORMATION, 2)), display.bonuses)
        assertEquals(4, display.total)
    }

    @Test
    fun `total comes from the backend param even when it differs from local sum`() {
        // Le backend fait foi : on retient sa valeur (10) même si base+bonus locaux feraient 3.
        val display =
            statDisplay("social", base = 2, total = 10, formation = null, peuple = peuple("social" to 1))

        assertEquals(listOf(StatBonus(StatBonusSource.PEUPLE, 1)), display.bonuses)
        assertEquals(10, display.total)
    }

    @Test
    fun `total falls back to base plus bonuses when backend total is null`() {
        val display = statDisplay(
            "social",
            base = 3,
            total = null,
            formation = formation("social", 2),
            peuple = peuple("social" to 1),
        )

        assertEquals(6, display.total)
    }
}
