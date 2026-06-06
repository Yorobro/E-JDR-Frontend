package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Implémentation par défaut de [LogoutUseCase].
 *
 * Orchestration pure : délègue la déconnexion à l'[AuthRepository] et renvoie son
 * résultat tel quel.
 */
class DefaultLogoutUseCase(
    private val authRepository: AuthRepository,
) : LogoutUseCase {
    override suspend fun invoke(): Result<Unit, AuthError> = authRepository.logout()
}
