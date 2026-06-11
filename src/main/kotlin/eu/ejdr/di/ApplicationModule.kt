package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.service.SessionService
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.auth.service.SessionServiceImpl
import eu.ejdr.application.features.auth.usecase.GetCurrentUserUseCaseImpl
import eu.ejdr.application.features.auth.usecase.LoginUseCaseImpl
import eu.ejdr.application.features.auth.usecase.LogoutUseCaseImpl
import eu.ejdr.application.features.auth.usecase.RegisterUseCaseImpl
import eu.ejdr.application.features.auth.usecase.RestoreSessionUseCaseImpl
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.features.settings.usecase.GetThemeUseCaseImpl
import eu.ejdr.application.features.settings.usecase.SetThemeUseCaseImpl
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.features.update.usecase.CheckUpdateUseCaseImpl
import eu.ejdr.application.features.update.usecase.DownloadAndInstallUpdateUseCaseImpl
import org.koin.dsl.module

/**
 * Module Koin de la couche application.
 *
 * Il enregistre les services et use cases métier en les liant à leurs interfaces
 * (ports d'entrée) : [SessionService], [LoginUseCase], [RegisterUseCase],
 * [RestoreSessionUseCase] et [LogoutUseCase] sont fournis via leurs
 * implémentations (suffixe `Impl`). Leurs dépendances infra (ex. [AuthRepository])
 * sont résolues depuis [infrastructureModule], la présentation ne connaissant
 * que ces interfaces.
 */
val applicationModule = module {
    single<SessionService> { SessionServiceImpl(get()) }
    single<LoginUseCase> { LoginUseCaseImpl(get()) }
    single<RegisterUseCase> { RegisterUseCaseImpl(get()) }
    single<RestoreSessionUseCase> { RestoreSessionUseCaseImpl(get()) }
    single<LogoutUseCase> { LogoutUseCaseImpl(get()) }
    single<GetCurrentUserUseCase> { GetCurrentUserUseCaseImpl(get()) }
    single<CheckUpdateUseCase> { CheckUpdateUseCaseImpl(get()) }
    single<DownloadAndInstallUpdateUseCase> { DownloadAndInstallUpdateUseCaseImpl(get(), get()) }
    single<GetThemeUseCase> { GetThemeUseCaseImpl(get()) }
    single<SetThemeUseCase> { SetThemeUseCaseImpl(get()) }
}
