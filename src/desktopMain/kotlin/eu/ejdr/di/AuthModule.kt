package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.auth.abstraction.service.SessionService
import eu.ejdr.application.features.auth.abstraction.usecase.ChangeEmailUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.ChangePasswordUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.auth.service.SessionServiceImpl
import eu.ejdr.application.features.auth.usecase.ChangeEmailUseCaseImpl
import eu.ejdr.application.features.auth.usecase.ChangePasswordUseCaseImpl
import eu.ejdr.application.features.auth.usecase.GetCurrentUserUseCaseImpl
import eu.ejdr.application.features.auth.usecase.LoginUseCaseImpl
import eu.ejdr.application.features.auth.usecase.LogoutUseCaseImpl
import eu.ejdr.application.features.auth.usecase.RegisterUseCaseImpl
import eu.ejdr.application.features.auth.usecase.RestoreSessionUseCaseImpl
import eu.ejdr.infrastructure.http.features.auth.AuthHttpMapper
import eu.ejdr.infrastructure.http.features.auth.AuthHttpRepository
import org.koin.dsl.module

/**
 * Module Koin de la feature authentification : ports application (use cases, service) +
 * adaptateurs infrastructure (repository HTTP, mapper). Découpage par feature, pour éviter
 * les god-modules.
 */
val authModule = module {
    single { AuthHttpMapper }
    single<AuthRepository> { AuthHttpRepository(get(), get(), get(), get<SessionPersistence>()) }
    single<SessionService> { SessionServiceImpl(get()) }
    single<LoginUseCase> { LoginUseCaseImpl(get()) }
    single<RegisterUseCase> { RegisterUseCaseImpl(get()) }
    single<RestoreSessionUseCase> { RestoreSessionUseCaseImpl(get()) }
    single<LogoutUseCase> { LogoutUseCaseImpl(get()) }
    single<GetCurrentUserUseCase> { GetCurrentUserUseCaseImpl(get()) }
    single<ChangeEmailUseCase> { ChangeEmailUseCaseImpl(get()) }
    single<ChangePasswordUseCase> { ChangePasswordUseCaseImpl(get()) }
}
