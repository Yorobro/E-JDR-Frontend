package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.usecase.ChangeEmailUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Implémentation de [ChangeEmailUseCase].
 *
 * Orchestration pure : délègue la modification de l'e-mail à l'[AuthRepository] et renvoie
 * son résultat tel quel.
 */
class ChangeEmailUseCaseImpl(
    private val authRepository: AuthRepository,
) : ChangeEmailUseCase {
    override suspend fun invoke(newEmail: String): Result<Unit, AuthError> =
        authRepository.changeEmail(newEmail)
}
