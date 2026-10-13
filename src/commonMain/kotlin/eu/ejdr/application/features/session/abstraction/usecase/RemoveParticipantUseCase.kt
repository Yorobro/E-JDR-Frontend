package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Use case : retire un joueur du salon d'attente.
 *
 * Contrepartie d'[InviteToLobbyUseCase], déclenché par le MJ depuis le lobby quand il s'est
 * trompé de joueur ou que celui-ci ne sera finalement pas de la partie. La participation est
 * supprimée, pas marquée : le joueur peut être reconvié ensuite via [InviteToLobbyUseCase]. Le
 * statut de la session ne change pas.
 */
fun interface RemoveParticipantUseCase {
    /**
     * @param sessionId identifiant de la session dont le lobby est ouvert.
     * @param userId identifiant du joueur à retirer.
     * @return le lobby complet à jour, sans le joueur retiré, ou une [SessionError].
     */
    suspend operator fun invoke(
        sessionId: String,
        userId: String,
    ): Result<SessionLobby, SessionError>
}
