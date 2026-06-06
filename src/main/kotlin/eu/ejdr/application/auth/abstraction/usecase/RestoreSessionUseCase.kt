package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError

fun interface RestoreSessionUseCase {
    suspend operator fun invoke(): Result<Unit, AuthError>
}
