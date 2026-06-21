package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError

/** Use case : récupère le détail d'une session. */
fun interface GetSessionUseCase {
    suspend operator fun invoke(sessionId: String): Result<Session, SessionError>
}
