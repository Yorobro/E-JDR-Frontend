package eu.ejdr.di

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.features.update.usecase.CheckUpdateUseCaseImpl
import eu.ejdr.application.features.update.usecase.DownloadAndInstallUpdateUseCaseImpl
import eu.ejdr.infrastructure.http.features.update.UpdateHttpRepository
import eu.ejdr.infrastructure.system.WindowsSystemLauncher
import org.koin.dsl.module

/** Module Koin de la feature mise à jour (vérification, téléchargement, lancement OS). */
val updateModule = module {
    single<UpdateRepository> { UpdateHttpRepository(get()) }
    single<SystemLauncherService> { WindowsSystemLauncher() }
    single<CheckUpdateUseCase> { CheckUpdateUseCaseImpl(get()) }
    single<DownloadAndInstallUpdateUseCase> { DownloadAndInstallUpdateUseCaseImpl(get(), get()) }
}
