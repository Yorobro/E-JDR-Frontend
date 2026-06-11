package eu.ejdr.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Destinations de navigation de l'application (Navigation 3).
 *
 * Chaque destination est une [NavKey] **sérialisable** : elle peut donc être placée
 * dans le back-stack possédé par l'application (cf. [eu.ejdr.presentation.App]) et,
 * à terme, restaurée. Les arguments d'un écran voyagent **dans la clé elle-même**
 * (ex. un futur `Campaign(id)`), ce qui remplace le passage manuel d'arguments via
 * des callbacks.
 *
 * Remplace l'ancienne `sealed interface Screen` + `NavActions` : la navigation se
 * fait désormais en empilant/dépilant ces clés sur le back-stack.
 */
sealed interface Route : NavKey {

    /** Écran de démarrage affiché pendant la tentative d'auto-login. */
    @Serializable
    data object Splash : Route

    /** Écran de connexion. */
    @Serializable
    data object Login : Route

    /** Écran d'inscription. */
    @Serializable
    data object Register : Route

    /** Zone connectée : écran d'accueil. */
    @Serializable
    data object Home : Route

    /** Écran des paramètres (thème, etc.). */
    @Serializable
    data object Settings : Route
}
