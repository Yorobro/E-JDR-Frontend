package eu.ejdr.application.features.auth.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Use case de modification de l'adresse e-mail de l'utilisateur connecté.
 *
 * S'invoque comme une fonction : `changeEmailUseCase(newEmail)`.
 *
 * @param newEmail Nouvelle adresse e-mail souhaitée.
 * @return [Result.Success] avec [Unit] si la modification est acceptée, sinon une [AuthError].
 */
fun interface ChangeEmailUseCase {
    suspend operator fun invoke(newEmail: String): Result<Unit, AuthError>
}
