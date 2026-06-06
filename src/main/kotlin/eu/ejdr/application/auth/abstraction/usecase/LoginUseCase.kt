package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

fun interface LoginUseCase {
    suspend operator fun invoke(credentials: Credentials): Result<User, AuthError>
}
