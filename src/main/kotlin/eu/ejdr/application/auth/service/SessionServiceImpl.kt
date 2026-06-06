package eu.ejdr.application.auth.service

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.service.SessionService
import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError

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

    override suspend fun restore(): Result<Unit, AuthError> {
        if (!authRepository.hasPersistedSession()) {
            return Result.Failure(AuthError.NoPersistedSession)
        }
        return authRepository.refresh()
    }
}
