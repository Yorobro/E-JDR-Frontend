package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError

class SetThemeUseCaseImpl(private val repository: ThemeRepository) : SetThemeUseCase {
    override suspend fun invoke(theme: ThemeVariant): Result<Unit, SettingsError> =
        repository.setTheme(theme)
}
