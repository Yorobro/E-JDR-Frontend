package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.service.SessionService
import eu.ejdr.application.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

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
