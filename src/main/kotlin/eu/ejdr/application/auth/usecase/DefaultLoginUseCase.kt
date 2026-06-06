package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Implémentation par défaut de [LoginUseCase].
 *
 * Orchestration pure : délègue la connexion à l'[AuthRepository] et renvoie son
 * résultat tel quel. Un use case ne contient pas de logique réutilisable et
 * n'appelle jamais un autre use case.
 */
class DefaultLoginUseCase(
    private val authRepository: AuthRepository,
) : LoginUseCase {
    override suspend fun invoke(credentials: Credentials): Result<User, AuthError> =
        authRepository.login(credentials)
}
