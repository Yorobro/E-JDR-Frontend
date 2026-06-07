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

    /**
     * Zone connectée : écran utilisateur affiché une fois l'utilisateur authentifié.
     *
     * @property user Profil connecté, ou `null` si arrivé par auto-login sans profil chargé.
     */
    data class User(val user: eu.ejdr.domain.entities.auth.User?) : Screen
    // NB: nom qualifié pour lever l'ambiguïté avec le nom de la data class elle-même.
}
