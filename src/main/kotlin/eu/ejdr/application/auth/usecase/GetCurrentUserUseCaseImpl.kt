package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Implémentation de [GetCurrentUserUseCase].
 *
 * Orchestration pure : délègue au [AuthRepository] et renvoie son résultat tel quel.
 * Un use case ne contient pas de logique réutilisable et n'appelle jamais un autre
 * use case.
 */
class GetCurrentUserUseCaseImpl(
    private val authRepository: AuthRepository,
) : GetCurrentUserUseCase {
    override suspend fun invoke(): Result<User, AuthError> = authRepository.me()
}
