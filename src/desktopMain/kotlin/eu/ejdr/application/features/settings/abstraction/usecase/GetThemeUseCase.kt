package eu.ejdr.application.features.settings.abstraction.usecase

import eu.ejdr.domain.features.settings.entities.ThemeVariant

fun interface GetThemeUseCase {
    suspend operator fun invoke(): ThemeVariant
}
