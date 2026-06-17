package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.error.SessionError

/** Use case : supprime une session. */
fun interface DeleteSessionUseCase {
    suspend operator fun invoke(sessionId: String): Result<Unit, SessionError>
}
