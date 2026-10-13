package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Use case : le joueur convié répond à une invitation de session.
 *
 * `accept = true` rejoint le lobby (INVITED → ACCEPTED) ; `false` décline (INVITED → REFUSED).
 */
fun interface RespondToInvitationUseCase {
    /**
     * @param sessionId identifiant de la session concernée.
     * @param accept `true` pour accepter, `false` pour refuser.
     * @return [Unit] en cas de succès, ou une [SessionError].
     */
    suspend operator fun invoke(sessionId: String, accept: Boolean): Result<Unit, SessionError>
}
