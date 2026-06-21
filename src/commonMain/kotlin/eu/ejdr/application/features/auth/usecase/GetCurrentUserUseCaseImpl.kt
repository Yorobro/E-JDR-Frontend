package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

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
