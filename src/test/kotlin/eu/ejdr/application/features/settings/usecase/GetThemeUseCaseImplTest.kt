package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class GetThemeUseCaseImplTest {

    private val repository = mockk<ThemeRepository>()
    private val useCase = GetThemeUseCaseImpl(repository)

    @Test
    fun `delegates to the repository and returns the persisted theme`() {
        every { repository.getTheme() } returns ThemeVariant.DARK

        assertEquals(ThemeVariant.DARK, useCase())
    }
}
