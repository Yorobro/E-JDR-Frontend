package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Implémentation de [LogoutUseCase].
 *
 * Orchestration pure : délègue la déconnexion à l'[AuthRepository] et renvoie son
 * résultat tel quel.
 */
class LogoutUseCaseImpl(
    private val authRepository: AuthRepository,
) : LogoutUseCase {
    override suspend fun invoke(): Result<Unit, AuthError> = authRepository.logout()
}
