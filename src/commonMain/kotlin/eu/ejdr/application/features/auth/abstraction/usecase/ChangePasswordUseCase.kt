package eu.ejdr.application.features.auth.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Use case de modification du mot de passe de l'utilisateur connecté.
 *
 * S'invoque comme une fonction : `changePasswordUseCase(currentPassword, newPassword)`.
 *
 * @param currentPassword Mot de passe actuel pour vérification.
 * @param newPassword Nouveau mot de passe souhaité.
 * @return [Result.Success] avec [Unit] si la modification est acceptée, sinon une [AuthError].
 */
fun interface ChangePasswordUseCase {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit, AuthError>
}
