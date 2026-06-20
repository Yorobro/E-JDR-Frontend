package eu.ejdr.presentation.features.charactersheet.component

import eu.ejdr.domain.features.charactersheet.entities.Purse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Couvre la normalisation monétaire pure de [Purse.formatNormalized] (1 PO = 100 PA = 10 000 PC).
 * Cette logique vivait dans un package exclu de la couverture ; elle est désormais comptée.
 */
class PurseFormatTest {

    @Test
    fun `formats an already-normalized purse unchanged`() {
        assertEquals("1 PO · 50 PA · 0 PC", Purse(gold = 1, silver = 50, copper = 0).formatNormalized())
    }

    @Test
    fun `carries copper overflow into silver`() {
        // 150 PC = 1 PA + 50 PC
        assertEquals("0 PO · 1 PA · 50 PC", Purse(gold = 0, silver = 0, copper = 150).formatNormalized())
    }

    @Test
    fun `carries silver overflow into gold`() {
        // 150 PA = 1 PO + 50 PA
        assertEquals("1 PO · 50 PA · 0 PC", Purse(gold = 0, silver = 150, copper = 0).formatNormalized())
    }

    @Test
    fun `cascades copper overflow all the way up to gold`() {
        // 10 000 PC = 1 PO
        assertEquals("1 PO · 0 PA · 0 PC", Purse(gold = 0, silver = 0, copper = 10_000).formatNormalized())
    }

    @Test
    fun `combines all three denominations with carry`() {
        // 1 PO + 99 PA + 250 PC = 1 PO + 99 PA + 2 PA + 50 PC = 1 PO + 101 PA + 50 PC
        //                       = 2 PO + 1 PA + 50 PC
        assertEquals("2 PO · 1 PA · 50 PC", Purse(gold = 1, silver = 99, copper = 250).formatNormalized())
    }

    @Test
    fun `formats an empty purse as all zeros`() {
        assertEquals("0 PO · 0 PA · 0 PC", Purse(gold = 0, silver = 0, copper = 0).formatNormalized())
    }
}
