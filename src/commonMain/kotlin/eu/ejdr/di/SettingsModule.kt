package eu.ejdr.di

import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.features.settings.usecase.GetThemeUseCaseImpl
import eu.ejdr.application.features.settings.usecase.SetThemeUseCaseImpl
import org.koin.dsl.module

/**
 * Module Koin de la feature paramètres (thème).
 *
 * Le binding de [eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository]
 * est platform-specific et vit dans le module d'infrastructure de chaque plateforme.
 */
val settingsModule = module {
    single<GetThemeUseCase> { GetThemeUseCaseImpl(get()) }
    single<SetThemeUseCase> { SetThemeUseCaseImpl(get()) }
}
