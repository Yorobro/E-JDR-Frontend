package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Use case : ouvre le lobby d'une session et convie les joueurs choisis.
 *
 * Déclenché par le MJ depuis le détail d'une session (`PLANNED` → `LOBBY`). Le MJ ne fait pas
 * partie de [participantUserIds] : il accède à la session via son rôle, sans s'inviter.
 */
fun interface CreateLobbyUseCase {
    /**
     * @param sessionId identifiant de la session à passer en lobby.
     * @param participantUserIds identifiants des joueurs conviés (hors MJ).
     * @return le lobby créé, ou une [SessionError].
     */
    suspend operator fun invoke(
        sessionId: String,
        participantUserIds: List<String>,
    ): Result<SessionLobby, SessionError>
}
