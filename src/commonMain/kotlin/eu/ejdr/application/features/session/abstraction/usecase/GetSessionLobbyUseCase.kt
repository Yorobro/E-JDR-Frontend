package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Use case : charge le lobby d'une session (statut + participants).
 *
 * Utilisé par le joueur qui rejoint le salon d'attente après avoir accepté, et par le MJ en
 * reprise à froid (le lobby n'existe sinon qu'en mémoire).
 */
fun interface GetSessionLobbyUseCase {
    /**
     * @param sessionId identifiant de la session.
     * @return le [SessionLobby] (session + participants), ou une [SessionError].
     */
    suspend operator fun invoke(sessionId: String): Result<SessionLobby, SessionError>
}
