package eu.ejdr.application.features.auth.service

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.service.SessionService
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

/**
 * Implémentation de [SessionService].
 *
 * Centralise la logique de rétablissement de session (vérification d'une session
 * persistée puis rafraîchissement) afin qu'elle soit partagée entre les use cases
 * plutôt que dupliquée. Un service peut s'appuyer sur des repositories et d'autres
 * services ; il ne dépend que des abstractions.
 */
class SessionServiceImpl(
    private val authRepository: AuthRepository,
) : SessionService {

    override fun hasPersistedSession(): Boolean = authRepository.hasPersistedSession()

    override suspend fun restore(): Result<User, AuthError> {
        if (!authRepository.hasPersistedSession()) {
            return Result.Failure(AuthError.NoPersistedSession)
        }
        return authRepository.refresh()
    }
}
