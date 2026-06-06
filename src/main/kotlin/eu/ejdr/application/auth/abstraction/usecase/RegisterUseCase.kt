package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Use case d'orchestration de la création d'un nouveau compte utilisateur.
 *
 * S'invoque comme une fonction : `registerUseCase(credentials)`.
 *
 * @param credentials identifiants du compte à créer
 * @return l'[User] nouvellement enregistré, ou une [AuthError] en cas d'échec
 */
fun interface RegisterUseCase {
    suspend operator fun invoke(credentials: Credentials): Result<User, AuthError>
}
