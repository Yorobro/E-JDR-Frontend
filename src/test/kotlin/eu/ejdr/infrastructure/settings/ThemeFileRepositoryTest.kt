package eu.ejdr.infrastructure.settings

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ThemeFileRepositoryTest {

    private val tmpDir = Files.createTempDirectory("ejdr-theme-test").toFile()
    private val repository = ThemeFileRepository(tmpDir)

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `defaults to LIGHT when nothing is persisted`() = runTest {
        assertEquals(ThemeVariant.LIGHT, repository.getTheme())
    }

    @Test
    fun `persists and reads back the theme`() = runTest {
        assertIs<Result.Success<Unit>>(repository.setTheme(ThemeVariant.DARK))
        assertEquals(ThemeVariant.DARK, repository.getTheme())
    }

    @Test
    fun `a fresh repository reads the previously persisted value`() = runTest {
        repository.setTheme(ThemeVariant.DARK)

        assertEquals(ThemeVariant.DARK, ThemeFileRepository(tmpDir).getTheme())
    }

    @Test
    fun `falls back to LIGHT on a corrupted theme value`() = runTest {
        File(tmpDir, "settings.properties").writeText("theme=NOT_A_THEME")

        assertEquals(ThemeVariant.LIGHT, repository.getTheme())
    }

    @Test
    fun `returns Failure when the data directory cannot be written`() = runTest {
        val filePath = Files.createTempFile("ejdr-not-a-dir", ".tmp").toFile()
        try {
            // dataDir pointe sur un fichier : impossible d'y créer "settings.properties".
            val brokenRepository = ThemeFileRepository(filePath)
            assertIs<Result.Failure<*>>(brokenRepository.setTheme(ThemeVariant.DARK))
        } finally {
            filePath.delete()
        }
    }
}
