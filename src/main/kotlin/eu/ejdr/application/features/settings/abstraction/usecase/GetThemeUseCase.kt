package eu.ejdr.application.features.settings.abstraction.usecase

import eu.ejdr.domain.features.settings.entities.ThemeVariant

fun interface GetThemeUseCase {
    operator fun invoke(): ThemeVariant
}
