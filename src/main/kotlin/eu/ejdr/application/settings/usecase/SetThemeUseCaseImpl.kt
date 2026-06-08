package eu.ejdr.application.settings.usecase

import eu.ejdr.application.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.settings.abstraction.ThemeVariant

class SetThemeUseCaseImpl(private val repository: ThemeRepository) : SetThemeUseCase {
    override fun invoke(theme: ThemeVariant) = repository.setTheme(theme)
}
