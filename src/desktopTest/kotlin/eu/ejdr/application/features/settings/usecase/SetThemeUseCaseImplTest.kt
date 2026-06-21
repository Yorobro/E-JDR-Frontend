package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class SetThemeUseCaseImplTest {

    private val repository = mockk<ThemeRepository>()
    private val useCase = SetThemeUseCaseImpl(repository)

    @Test
    fun `returns Success when the repository persists the theme`() = runTest {
        coEvery { repository.setTheme(ThemeVariant.GRIMOIRE) } returns Result.Success(Unit)

        val result = useCase(ThemeVariant.GRIMOIRE)

        assertIs<Result.Success<Unit>>(result)
        coVerify { repository.setTheme(ThemeVariant.GRIMOIRE) }
    }

    @Test
    fun `propagates Failure with ThemePersistenceFailed when the repository fails`() = runTest {
        coEvery { repository.setTheme(ThemeVariant.PARCHEMIN) } returns
            Result.Failure(SettingsError.ThemePersistenceFailed)

        val result = useCase(ThemeVariant.PARCHEMIN)

        assertIs<Result.Failure<SettingsError>>(result)
        assert(result.error == SettingsError.ThemePersistenceFailed)
    }
}
