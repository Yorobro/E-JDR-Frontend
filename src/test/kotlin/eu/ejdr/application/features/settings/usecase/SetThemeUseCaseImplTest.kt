package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.error.SettingsError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertIs

class SetThemeUseCaseImplTest {

    private val repository = mockk<ThemeRepository>()
    private val useCase = SetThemeUseCaseImpl(repository)

    @Test
    fun `returns Success when the repository persists the theme`() {
        every { repository.setTheme(ThemeVariant.DARK) } returns true

        val result = useCase(ThemeVariant.DARK)

        assertIs<Result.Success<Unit>>(result)
        verify { repository.setTheme(ThemeVariant.DARK) }
    }

    @Test
    fun `returns Failure with ThemePersistenceFailed when the write fails`() {
        every { repository.setTheme(ThemeVariant.LIGHT) } returns false

        val result = useCase(ThemeVariant.LIGHT)

        assertIs<Result.Failure<SettingsError>>(result)
        assert(result.error == SettingsError.ThemePersistenceFailed)
    }
}
