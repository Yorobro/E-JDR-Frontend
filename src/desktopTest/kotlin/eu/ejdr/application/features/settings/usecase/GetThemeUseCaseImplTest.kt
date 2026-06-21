package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetThemeUseCaseImplTest {

    private val repository = mockk<ThemeRepository>()
    private val useCase = GetThemeUseCaseImpl(repository)

    @Test
    fun `delegates to the repository and returns the persisted theme`() = runTest {
        coEvery { repository.getTheme() } returns ThemeVariant.DARK

        assertEquals(ThemeVariant.DARK, useCase())
    }
}
