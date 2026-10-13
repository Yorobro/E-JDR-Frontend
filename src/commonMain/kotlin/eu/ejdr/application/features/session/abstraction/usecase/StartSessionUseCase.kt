package eu.ejdr.application.features.session.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Use case : le MJ démarre réellement la session (transition `LOBBY → ACTIVE`).
 *
 * Déclenché quand le MJ confirme que tous les joueurs sont présents dans le lobby. Le backend
 * notifie alors le groupe pour faire basculer le MJ et les joueurs vers l'écran de jeu.
 */
fun interface StartSessionUseCase {
    /**
     * @param sessionId identifiant de la session à démarrer.
     * @return [Unit] en cas de succès, ou une [SessionError].
     */
    suspend operator fun invoke(sessionId: String): Result<Unit, SessionError>
}
