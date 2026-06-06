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

val applicationModule = module {
    single<SessionService> { DefaultSessionService(get()) }
    single<LoginUseCase> { DefaultLoginUseCase(get()) }
    single<RegisterUseCase> { DefaultRegisterUseCase(get()) }
    single<RestoreSessionUseCase> { DefaultRestoreSessionUseCase(get()) }
    single<LogoutUseCase> { DefaultLogoutUseCase(get()) }
}
