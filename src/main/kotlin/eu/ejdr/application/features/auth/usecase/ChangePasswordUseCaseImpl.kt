package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.usecase.ChangePasswordUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Implémentation de [ChangePasswordUseCase].
 *
 * Orchestration pure : délègue la modification du mot de passe à l'[AuthRepository] et renvoie
 * son résultat tel quel.
 */
class ChangePasswordUseCaseImpl(
    private val authRepository: AuthRepository,
) : ChangePasswordUseCase {
    override suspend fun invoke(currentPassword: String, newPassword: String): Result<Unit, AuthError> =
        authRepository.changePassword(currentPassword, newPassword)
}
