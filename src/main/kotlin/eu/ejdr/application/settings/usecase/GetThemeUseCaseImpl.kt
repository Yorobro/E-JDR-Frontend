package eu.ejdr.application.settings.usecase

import eu.ejdr.application.settings.abstraction.ThemeVariant
import eu.ejdr.application.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.settings.abstraction.usecase.GetThemeUseCase

class GetThemeUseCaseImpl(private val repository: ThemeRepository) : GetThemeUseCase {
    override fun invoke(): ThemeVariant = repository.getTheme()
}
