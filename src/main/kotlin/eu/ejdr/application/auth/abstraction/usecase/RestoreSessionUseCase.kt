package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Use case d'auto-login silencieux au démarrage de l'application.
 *
 * Tente de rétablir la session précédente à partir des informations persistées,
 * sans intervention de l'utilisateur. Délègue la logique réutilisable au
 * `SessionService`. S'invoque comme une fonction : `restoreSessionUseCase()`.
 *
 * @return l'[User] dont la session est rétablie, ou une [AuthError] (ex. absence de
 *   session persistée) sinon
 */
fun interface RestoreSessionUseCase {
    suspend operator fun invoke(): Result<User, AuthError>
}
