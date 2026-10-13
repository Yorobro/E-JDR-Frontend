package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Use case : convie des joueurs à un salon d'attente **déjà ouvert**.
 *
 * Déclenché par le MJ depuis le lobby, quand il a oublié un joueur ou qu'un joueur a refusé par
 * erreur (celui-ci repasse alors en attente de réponse). Contrairement à [CreateLobbyUseCase],
 * le statut de la session ne change pas : seules les participations évoluent.
 */
fun interface InviteToLobbyUseCase {
    /**
     * @param sessionId identifiant de la session dont le lobby est ouvert.
     * @param participantUserIds identifiants des joueurs à convier (hors MJ).
     * @return le lobby complet à jour, ou une [SessionError].
     */
    suspend operator fun invoke(
        sessionId: String,
        participantUserIds: List<String>,
    ): Result<SessionLobby, SessionError>
}
