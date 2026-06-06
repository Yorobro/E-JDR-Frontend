package eu.ejdr.application.auth.abstraction.repository

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

interface AuthRepository {
    suspend fun login(credentials: Credentials): Result<User, AuthError>
    suspend fun register(credentials: Credentials): Result<User, AuthError>
    suspend fun refresh(): Result<Unit, AuthError>
    suspend fun logout(): Result<Unit, AuthError>
    fun hasPersistedSession(): Boolean
}
