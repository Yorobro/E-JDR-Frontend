package eu.ejdr.di

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.features.settings.usecase.GetThemeUseCaseImpl
import eu.ejdr.application.features.settings.usecase.SetThemeUseCaseImpl
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.settings.ThemeFileRepository
import org.koin.dsl.module
import java.io.File

/** Module Koin de la feature paramètres (thème). */
val settingsModule = module {
    single<ThemeRepository> { ThemeFileRepository(File(get<AppConfig>().dataDir)) }
    single<GetThemeUseCase> { GetThemeUseCaseImpl(get()) }
    single<SetThemeUseCase> { SetThemeUseCaseImpl(get()) }
}
