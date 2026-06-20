package eu.ejdr.application.features.session.usecase

import eu.ejdr.application.features.session.abstraction.repository.SessionRepository
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Implémentations des use cases de la feature sessions.
 *
 * Orchestration triviale : chaque use case délègue au [SessionRepository]. Regroupées dans un
 * même fichier (comme les fiches de personnage) car chacune est une simple délégation.
 */

class ListCampaignSessionsUseCaseImpl(
    private val repository: SessionRepository,
) : ListCampaignSessionsUseCase {
    override suspend fun invoke(campaignId: String): Result<List<Session>, SessionError> =
        repository.listByCampaign(campaignId)
}

class CreateSessionUseCaseImpl(
    private val repository: SessionRepository,
) : CreateSessionUseCase {
    override suspend fun invoke(
        campaignId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError> = repository.create(campaignId, title, date)
}

class GetSessionUseCaseImpl(
    private val repository: SessionRepository,
) : GetSessionUseCase {
    override suspend fun invoke(sessionId: String): Result<Session, SessionError> =
        repository.get(sessionId)
}

class UpdateSessionUseCaseImpl(
    private val repository: SessionRepository,
) : UpdateSessionUseCase {
    override suspend fun invoke(
        sessionId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError> = repository.update(sessionId, title, date)
}

class DeleteSessionUseCaseImpl(
    private val repository: SessionRepository,
) : DeleteSessionUseCase {
    override suspend fun invoke(sessionId: String): Result<Unit, SessionError> =
        repository.delete(sessionId)
}
