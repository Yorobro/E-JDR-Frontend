package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.ThemeVariant

class SetThemeUseCaseImpl(private val repository: ThemeRepository) : SetThemeUseCase {
    override fun invoke(theme: ThemeVariant) = repository.setTheme(theme)
}
