package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.service.SessionService
import eu.ejdr.application.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError

class DefaultRestoreSessionUseCase(
    private val sessionService: SessionService,
) : RestoreSessionUseCase {
    override suspend fun invoke(): Result<Unit, AuthError> = sessionService.restore()
}
