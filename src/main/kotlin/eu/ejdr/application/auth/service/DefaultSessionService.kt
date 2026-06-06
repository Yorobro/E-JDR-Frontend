package eu.ejdr.application.auth.service

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.service.SessionService
import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError

class DefaultSessionService(
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
