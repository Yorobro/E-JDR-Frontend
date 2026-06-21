package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError

/** Use case : crée une session dans une campagne. */
fun interface CreateSessionUseCase {
    suspend operator fun invoke(
        campaignId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError>
}
