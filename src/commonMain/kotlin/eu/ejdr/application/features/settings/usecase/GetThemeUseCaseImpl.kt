package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.domain.features.settings.entities.ThemeVariant

class GetThemeUseCaseImpl(private val repository: ThemeRepository) : GetThemeUseCase {
    override suspend fun invoke(): ThemeVariant = repository.getTheme()
}
