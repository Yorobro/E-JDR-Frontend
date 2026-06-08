package eu.ejdr.application.settings.abstraction.usecase

import eu.ejdr.application.settings.abstraction.ThemeVariant

fun interface SetThemeUseCase {
    operator fun invoke(theme: ThemeVariant)
}
