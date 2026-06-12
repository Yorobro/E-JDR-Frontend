package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.error.SettingsError

class SetThemeUseCaseImpl(private val repository: ThemeRepository) : SetThemeUseCase {
    override fun invoke(theme: ThemeVariant): Result<Unit, SettingsError> =
        if (repository.setTheme(theme)) {
            Result.Success(Unit)
        } else {
            Result.Failure(SettingsError.ThemePersistenceFailed)
        }
}
