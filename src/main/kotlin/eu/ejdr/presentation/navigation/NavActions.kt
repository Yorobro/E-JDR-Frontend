package eu.ejdr.presentation.navigation

import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.domain.features.auth.entities.User

/**
 * Regroupe l'ensemble des callbacks de navigation passés à [AppRouter].
 *
 * Évite une liste de paramètres lambda trop longue dans la signature du routeur tout en gardant
 * les callbacks explicites et nommés. Chaque propriété est une lambda sans état : elle ne fait que
 * déclencher un changement d'état dans [eu.ejdr.presentation.App].
 *
 * @property onAuthenticated Appelé après login ou inscription réussis, avec l'[User] connecté.
 * @property onGoToRegister  Navigation vers l'écran d'inscription depuis le login.
 * @property onGoToLogin     Navigation vers l'écran de connexion depuis l'inscription.
 * @property onLogout        Déconnexion : invalide la session et revient à [Screen.Login].
 * @property onSettings      Ouvre [Screen.Settings] depuis la zone connectée.
 * @property onBack          Retour depuis [Screen.Settings] vers [Screen.Home].
 * @property onThemeChange   Applique le [ThemeVariant] sélectionné dans les paramètres.
 * @property onSessionExpired Rappelé lorsqu'une route connectée détecte l'expiration de session.
 */
data class NavActions(
    val onAuthenticated: (User) -> Unit,
    val onGoToRegister: () -> Unit,
    val onGoToLogin: () -> Unit,
    val onLogout: () -> Unit,
    val onSettings: () -> Unit,
    val onBack: () -> Unit,
    val onThemeChange: (ThemeVariant) -> Unit,
    val onSessionExpired: () -> Unit,
)
