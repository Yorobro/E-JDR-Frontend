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
 * [runCatchingCancellable] et toute exception est convertie en erreur de domaine — aucune ne
 * traverse vers la présentation. Un échec d'intégrité (binaire non vérifié : [SecurityException]
 * levée par le repository) est distingué en [UpdateError.IntegrityCheckFailed] pour que rien de
 * non vérifié ne soit jamais lancé ; tout autre échec (réseau, écriture disque) reste
 * [UpdateError.DownloadFailed]. En cas de succès, le contrôle est cédé au launcher dont le retour
 * est `Nothing` : en production il **quitte la JVM** et cette ligne ne rend jamais la main. La
 * signature `Result<Unit, _>` est conservée pour l'homogénéité avec les autres use cases.
 */
class DownloadAndInstallUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val systemLauncher: SystemLauncherService,
) : DownloadAndInstallUpdateUseCase {
    override suspend fun invoke(
        downloadUrl: String,
        sha256Url: String?,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError> {
        val installer = runCatchingCancellable {
            updateRepository.downloadUpdate(downloadUrl, sha256Url, onProgress)
        }.getOrElse { error ->
            val reason =
                if (error is SecurityException) UpdateError.IntegrityCheckFailed
                else UpdateError.DownloadFailed
            return Result.Failure(reason)
        }

        return systemLauncher.launchInstallerAndExit(installer)
    }
}
