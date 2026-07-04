package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError

fun interface CheckUpdateUseCase {
    /**
     * Vérifie la disponibilité d'une mise à jour plus récente.
     *
     * @return [Result.Success] portant l'info de MAJ, ou `null` si l'app est à jour ;
     * [UpdateError.CheckFailed] si la vérification échoue (réseau).
     */
    suspend operator fun invoke(): Result<UpdateInfoDto?, UpdateError>
}
