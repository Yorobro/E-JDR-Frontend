package eu.ejdr.application.features.settings.abstraction.usecase

import eu.ejdr.application.features.settings.abstraction.ThemeVariant

fun interface GetThemeUseCase {
    operator fun invoke(): ThemeVariant
}
