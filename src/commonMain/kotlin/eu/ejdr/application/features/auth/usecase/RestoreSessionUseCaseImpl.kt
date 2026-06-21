package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.service.SessionService
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Implémentation de [RestoreSessionUseCase].
 *
 * Orchestration pure : délègue la restauration de session au [SessionService],
 * illustrant la réutilisation de logique via un service plutôt que par appel
 * d'un autre use case.
 */
class RestoreSessionUseCaseImpl(
    private val sessionService: SessionService,
) : RestoreSessionUseCase {
    override suspend fun invoke(): Result<User, AuthError> = sessionService.restore()
}
