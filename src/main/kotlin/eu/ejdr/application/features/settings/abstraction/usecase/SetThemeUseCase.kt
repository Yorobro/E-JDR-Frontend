package eu.ejdr.application.features.settings.abstraction.usecase

import eu.ejdr.application.features.settings.abstraction.ThemeVariant

fun interface SetThemeUseCase {
    operator fun invoke(theme: ThemeVariant)
}
