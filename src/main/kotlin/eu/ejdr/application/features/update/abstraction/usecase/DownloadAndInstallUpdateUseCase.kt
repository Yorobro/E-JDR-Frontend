package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError

fun interface DownloadAndInstallUpdateUseCase {
    /**
     * Télécharge puis lance l'installeur de la mise à jour.
     *
     * @return [Result.Success] si le lancement a été déclenché, ou
     * [UpdateError.DownloadFailed] en cas d'échec (réseau, écriture disque, lancement OS).
     */
    suspend operator fun invoke(
        downloadUrl: String,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError>
}
