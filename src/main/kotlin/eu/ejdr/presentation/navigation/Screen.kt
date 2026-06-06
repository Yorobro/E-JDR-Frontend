package eu.ejdr.presentation.navigation

/**
 * État de navigation de l'application.
 *
 * La navigation se fait par état : l'écran courant est représenté par une instance de cette
 * interface scellée, et changer d'écran revient à changer cette valeur d'état (voir [eu.ejdr.presentation.App]).
 */
sealed interface Screen {
    /** Écran de démarrage affiché pendant la tentative d'auto-login (restauration de session). */
    data object Splash : Screen

    /** Écran de connexion. */
    data object Login : Screen

    /** Écran d'inscription. */
    data object Register : Screen

    /** Écran d'accueil affiché une fois l'utilisateur authentifié. */
    data object Home : Screen
}
