package eu.ejdr.di

import eu.ejdr.application.auth.abstraction.service.SessionService
import eu.ejdr.application.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.auth.service.DefaultSessionService
import eu.ejdr.application.auth.usecase.DefaultLoginUseCase
import eu.ejdr.application.auth.usecase.DefaultLogoutUseCase
import eu.ejdr.application.auth.usecase.DefaultRegisterUseCase
import eu.ejdr.application.auth.usecase.DefaultRestoreSessionUseCase
import org.koin.dsl.module

/**
 * Module Koin de la couche application.
 *
 * Il enregistre les services et use cases métier en les liant à leurs interfaces
 * (ports d'entrée) : [SessionService], [LoginUseCase], [RegisterUseCase],
 * [RestoreSessionUseCase] et [LogoutUseCase] sont fournis via leurs
 * implémentations par défaut. Leurs dépendances infra (ex. [AuthRepository])
 * sont résolues depuis [infrastructureModule], la présentation ne connaissant
 * que ces interfaces.
 */
val applicationModule = module {
    single<SessionService> { DefaultSessionService(get()) }
    single<LoginUseCase> { DefaultLoginUseCase(get()) }
    single<RegisterUseCase> { DefaultRegisterUseCase(get()) }
    single<RestoreSessionUseCase> { DefaultRestoreSessionUseCase(get()) }
    single<LogoutUseCase> { DefaultLogoutUseCase(get()) }
}
