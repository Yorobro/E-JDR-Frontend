package eu.ejdr.application.features.session.usecase

import eu.ejdr.application.features.session.abstraction.repository.SessionRepository
import eu.ejdr.application.features.session.abstraction.usecase.CreateLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListMySessionInvitationsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.RespondToInvitationUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.entities.SessionInvitation
import eu.ejdr.domain.features.session.entities.SessionLobby
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

class CreateLobbyUseCaseImpl(
    private val repository: SessionRepository,
) : CreateLobbyUseCase {
    override suspend fun invoke(
        sessionId: String,
        participantUserIds: List<String>,
    ): Result<SessionLobby, SessionError> = repository.createLobby(sessionId, participantUserIds)
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

class RespondToInvitationUseCaseImpl(
    private val repository: SessionRepository,
) : RespondToInvitationUseCase {
    override suspend fun invoke(sessionId: String, accept: Boolean): Result<Unit, SessionError> =
        repository.respondToInvitation(sessionId, accept)
}

class ListMySessionInvitationsUseCaseImpl(
    private val repository: SessionRepository,
) : ListMySessionInvitationsUseCase {
    override suspend fun invoke(): Result<List<SessionInvitation>, SessionError> =
        repository.listMyInvitations()
}

class GetSessionLobbyUseCaseImpl(
    private val repository: SessionRepository,
) : GetSessionLobbyUseCase {
    override suspend fun invoke(sessionId: String): Result<SessionLobby, SessionError> =
        repository.getLobby(sessionId)
}
