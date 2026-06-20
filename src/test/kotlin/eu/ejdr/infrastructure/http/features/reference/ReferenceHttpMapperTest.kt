package eu.ejdr.infrastructure.http.features.reference

import eu.ejdr.infrastructure.http.features.reference.dto.ReferenceItemDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests du mapper de transport [ReferenceHttpMapper] : conversion DTO → domaine. Vérifie en
 * particulier la propagation des champs optionnels (stat/bonus/compétences/points de protection).
 */
class ReferenceHttpMapperTest {

    @Test
    fun `toItem propagates all fields including protection points`() {
        val dto = ReferenceItemDto(
            id = "a-1",
            name = "Cotte de mailles",
            createdAt = "2026-06-13T10:00:00.000Z",
            stat = "vigueur",
            bonus = 2,
            competenceIds = listOf("c-1", "c-2"),
            protectionPoints = 5,
            description = "Protège efficacement le torse.",
        )

        val item = ReferenceHttpMapper.toItem(dto)

        assertEquals("a-1", item.id)
        assertEquals("Cotte de mailles", item.name)
        assertEquals("2026-06-13T10:00:00.000Z", item.createdAt)
        assertEquals("vigueur", item.stat)
        assertEquals(2, item.bonus)
        assertEquals(listOf("c-1", "c-2"), item.competenceIds)
        assertEquals(5, item.protectionPoints)
        assertEquals("Protège efficacement le torse.", item.description)
    }

    @Test
    fun `toItem leaves optional fields null or empty when absent`() {
        val dto = ReferenceItemDto(id = "x-1", name = "Épée", createdAt = "2026-06-13T10:00:00.000Z")

        val item = ReferenceHttpMapper.toItem(dto)

        assertNull(item.stat)
        assertNull(item.bonus)
        assertTrue(item.competenceIds.isEmpty())
        assertNull(item.protectionPoints)
        assertNull(item.description)
    }
}
