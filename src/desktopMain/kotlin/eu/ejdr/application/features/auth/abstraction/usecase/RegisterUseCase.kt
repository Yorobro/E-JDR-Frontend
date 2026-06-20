package eu.ejdr.application.features.auth.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

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
