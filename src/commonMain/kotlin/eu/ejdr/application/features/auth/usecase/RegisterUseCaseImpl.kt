package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Implémentation de [RegisterUseCase].
 *
 * Orchestration pure : délègue la création de compte à l'[AuthRepository] et
 * renvoie son résultat tel quel.
 */
class RegisterUseCaseImpl(
    private val authRepository: AuthRepository,
) : RegisterUseCase {
    override suspend fun invoke(credentials: Credentials): Result<User, AuthError> =
        authRepository.register(credentials)
}
