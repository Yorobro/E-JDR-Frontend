package eu.ejdr.domain.shared.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests du value object [SemanticVersion] : c'est ici que vit désormais la logique de
 * comparaison de versions, sortie du use case `CheckUpdate` pour être testable isolément.
 */
class SemanticVersionTest {

    @Test
    fun `parses a plain x dot y dot z version`() {
        val v = SemanticVersion.parse("1.2.3")
        assertEquals(SemanticVersion(1, 2, 3), v)
    }

    @Test
    fun `ignores a leading v prefix`() {
        assertEquals(SemanticVersion(1, 2, 3), SemanticVersion.parse("v1.2.3"))
    }

    @Test
    fun `treats a missing patch component as zero`() {
        assertEquals(SemanticVersion(1, 1, 0), SemanticVersion.parse("v1.1"))
    }

    @Test
    fun `treats a missing minor and patch as zero`() {
        assertEquals(SemanticVersion(2, 0, 0), SemanticVersion.parse("2"))
    }

    @Test
    fun `treats non-numeric components as zero`() {
        assertEquals(SemanticVersion(1, 0, 0), SemanticVersion.parse("v1.x.y"))
    }

    @Test
    fun `equal versions are not newer than each other`() {
        assertFalse(SemanticVersion.parse("1.0.0").isNewerThan(SemanticVersion.parse("1.0.0")))
    }

    @Test
    fun `older patch is not newer`() {
        assertFalse(SemanticVersion.parse("1.0.0").isNewerThan(SemanticVersion.parse("1.0.1")))
    }

    @Test
    fun `older minor is not newer`() {
        assertFalse(SemanticVersion.parse("1.0.0").isNewerThan(SemanticVersion.parse("1.1.0")))
    }

    @Test
    fun `older major is not newer`() {
        assertFalse(SemanticVersion.parse("1.0.0").isNewerThan(SemanticVersion.parse("2.0.0")))
    }

    @Test
    fun `newer patch is newer`() {
        assertTrue(SemanticVersion.parse("1.0.1").isNewerThan(SemanticVersion.parse("1.0.0")))
    }

    @Test
    fun `newer minor wins over a higher patch`() {
        assertTrue(SemanticVersion.parse("1.1.0").isNewerThan(SemanticVersion.parse("1.0.9")))
    }

    @Test
    fun `newer major wins over higher minor and patch`() {
        assertTrue(SemanticVersion.parse("2.0.0").isNewerThan(SemanticVersion.parse("1.9.9")))
    }
}
