package eu.ejdr.application.features.auth.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

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
