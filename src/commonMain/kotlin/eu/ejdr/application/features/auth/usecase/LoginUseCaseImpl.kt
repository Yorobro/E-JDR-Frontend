package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Implémentation de [LoginUseCase].
 *
 * Orchestration pure : délègue la connexion à l'[AuthRepository] et renvoie son
 * résultat tel quel. Un use case ne contient pas de logique réutilisable et
 * n'appelle jamais un autre use case.
 */
class LoginUseCaseImpl(
    private val authRepository: AuthRepository,
) : LoginUseCase {
    override suspend fun invoke(credentials: Credentials): Result<User, AuthError> =
        authRepository.login(credentials)
}
