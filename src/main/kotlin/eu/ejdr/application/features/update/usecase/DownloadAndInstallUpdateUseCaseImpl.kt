package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase

/**
 * Implémentation de [DownloadAndInstallUpdateUseCase].
 *
 * Orchestration pure : télécharge l'installeur via l'[UpdateRepository] puis délègue
 * le lancement et la fermeture de l'application au service [SystemLauncherService]. Les
 * effets de bord OS (processus externe, exit JVM) sont ainsi tenus hors de la couche
 * application, ce qui rend ce use case testable (le launcher est mockable).
 */
class DownloadAndInstallUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val systemLauncher: SystemLauncherService,
) : DownloadAndInstallUpdateUseCase {
    override suspend fun invoke(downloadUrl: String, onProgress: (Float?) -> Unit) {
        val installer = updateRepository.downloadUpdate(downloadUrl, onProgress)
        systemLauncher.launchInstallerAndExit(installer)
    }
}
