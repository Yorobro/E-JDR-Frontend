package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Use case d'orchestration de la déconnexion de l'utilisateur courant.
 *
 * S'invoque comme une fonction : `logoutUseCase()`.
 *
 * @return [Unit] si la déconnexion réussit, ou une [AuthError] en cas d'échec
 */
fun interface LogoutUseCase {
    suspend operator fun invoke(): Result<Unit, AuthError>
}
