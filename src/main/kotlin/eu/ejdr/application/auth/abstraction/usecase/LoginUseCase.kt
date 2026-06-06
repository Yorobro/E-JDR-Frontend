package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Use case d'orchestration de la connexion d'un utilisateur existant.
 *
 * S'invoque comme une fonction : `loginUseCase(credentials)`.
 *
 * @param credentials identifiants saisis par l'utilisateur
 * @return l'[User] authentifié, ou une [AuthError] en cas d'échec
 */
fun interface LoginUseCase {
    suspend operator fun invoke(credentials: Credentials): Result<User, AuthError>
}
