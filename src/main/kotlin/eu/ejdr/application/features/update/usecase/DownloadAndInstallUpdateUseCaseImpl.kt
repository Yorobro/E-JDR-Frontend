package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.update.error.UpdateError

/**
 * Implémentation de [DownloadAndInstallUpdateUseCase].
 *
 * Orchestration pure : télécharge l'installeur via l'[UpdateRepository] puis délègue le
 * lancement et la fermeture de l'application au service [SystemLauncherService]. Les effets
 * de bord OS sont tenus hors de la couche application (testable).
 *
 * Seul le téléchargement est faillible de façon **récupérable** : il est enveloppé dans
 * [runCatchingCancellable] et toute exception (réseau, écriture disque) est convertie en
 * [UpdateError.DownloadFailed] — aucune ne traverse vers la présentation. En cas de succès,
 * le contrôle est cédé au launcher dont le retour est `Nothing` : en production il **quitte
 * la JVM** et cette ligne ne rend jamais la main. La signature `Result<Unit, _>` est conservée
 * pour l'homogénéité avec les autres use cases.
 */
class DownloadAndInstallUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val systemLauncher: SystemLauncherService,
) : DownloadAndInstallUpdateUseCase {
    override suspend fun invoke(
        downloadUrl: String,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError> {
        val installer = runCatchingCancellable {
            updateRepository.downloadUpdate(downloadUrl, onProgress)
        }.getOrElse { return Result.Failure(UpdateError.DownloadFailed) }

        return systemLauncher.launchInstallerAndExit(installer)
    }
}
