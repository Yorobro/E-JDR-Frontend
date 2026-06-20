package eu.ejdr.application.features.auth.abstraction.service

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError

/** Service réutilisable encapsulant la restauration silencieuse de session. */
interface SessionService {
    fun hasPersistedSession(): Boolean
    suspend fun restore(): Result<User, AuthError>
}
