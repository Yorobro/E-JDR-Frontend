package eu.ejdr.di

import eu.ejdr.application.auth.abstraction.service.SessionService
import eu.ejdr.application.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.auth.service.SessionServiceImpl
import eu.ejdr.application.auth.usecase.LoginUseCaseImpl
import eu.ejdr.application.auth.usecase.LogoutUseCaseImpl
import eu.ejdr.application.auth.usecase.RegisterUseCaseImpl
import eu.ejdr.application.auth.usecase.RestoreSessionUseCaseImpl
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
}
