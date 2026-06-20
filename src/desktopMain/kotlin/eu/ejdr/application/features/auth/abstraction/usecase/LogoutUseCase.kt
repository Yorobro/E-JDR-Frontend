package eu.ejdr.application.features.auth.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.error.AuthError

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
