package eu.ejdr.application.auth.abstraction.service

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/** Service réutilisable encapsulant la restauration silencieuse de session. */
interface SessionService {
    fun hasPersistedSession(): Boolean
    suspend fun restore(): Result<User, AuthError>
}
