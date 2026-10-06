package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.SessionInvitation
import eu.ejdr.domain.features.session.error.SessionError

/** Use case : liste les invitations de session en attente du joueur courant. */
fun interface ListMySessionInvitationsUseCase {
    /**
     * @return les invitations en attente (sessions en `LOBBY` où l'utilisateur est `INVITED`),
     *         ou une [SessionError].
     */
    suspend operator fun invoke(): Result<List<SessionInvitation>, SessionError>
}
