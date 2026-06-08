package eu.ejdr.application.settings.abstraction.usecase

import eu.ejdr.application.settings.abstraction.ThemeVariant

fun interface GetThemeUseCase {
    operator fun invoke(): ThemeVariant
}
