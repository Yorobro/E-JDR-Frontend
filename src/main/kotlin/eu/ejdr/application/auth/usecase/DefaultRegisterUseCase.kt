package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

class DefaultRegisterUseCase(
    private val authRepository: AuthRepository,
) : RegisterUseCase {
    override suspend fun invoke(credentials: Credentials): Result<User, AuthError> =
        authRepository.register(credentials)
}
