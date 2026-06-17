package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError

/** Use case : met à jour le titre et la date d'une session. */
fun interface UpdateSessionUseCase {
    suspend operator fun invoke(
        sessionId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError>
}
