package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase

class GetThemeUseCaseImpl(private val repository: ThemeRepository) : GetThemeUseCase {
    override fun invoke(): ThemeVariant = repository.getTheme()
}
