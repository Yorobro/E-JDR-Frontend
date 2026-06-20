package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError

fun interface DownloadAndInstallUpdateUseCase {
    /**
     * Télécharge, **vérifie l'intégrité**, puis lance l'installeur de la mise à jour.
     *
     * @param downloadUrl URL de l'installeur (doit être un hôte GitHub de confiance).
     * @param sha256Url URL de l'empreinte SHA-256 publiée ; son absence bloque l'installation.
     * @return [Result.Success] si le lancement a été déclenché ;
     * [UpdateError.IntegrityCheckFailed] si l'intégrité n'a pu être garantie ;
     * [UpdateError.DownloadFailed] sur autre échec (réseau, écriture disque, lancement OS).
     */
    suspend operator fun invoke(
        downloadUrl: String,
        sha256Url: String?,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError>
}
