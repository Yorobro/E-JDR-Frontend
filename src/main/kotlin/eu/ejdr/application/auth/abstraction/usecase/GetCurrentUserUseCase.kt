package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Use case de consultation du profil de l'utilisateur courant (`GET /me`).
 *
 * S'invoque comme une fonction : `getCurrentUserUseCase()`.
 *
 * @return l'[User] courant, ou une [AuthError] ([AuthError.SessionExpired] si la
 * session n'est plus valide côté serveur)
 */
fun interface GetCurrentUserUseCase {
    suspend operator fun invoke(): Result<User, AuthError>
}
