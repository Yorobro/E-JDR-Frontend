package eu.ejdr.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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

    /** Liste des campagnes dont l'utilisateur est le maître du jeu. */
    @Serializable
    data object Campaigns : Route

    /**
     * Détail d'une campagne. Les arguments voyagent dans la clé elle-même.
     *
     * @property id Identifiant de la campagne.
     * @property name Nom de la campagne (affiché en titre, évite un appel réseau).
     */
    @Serializable
    data class CampaignDetail(val id: String, val name: String) : Route

    /** Liste des fiches de personnage de l'utilisateur. */
    @Serializable
    data object CharacterSheets : Route
}

/**
 * Configuration de sérialisation du back-stack Navigation 3.
 *
 * `rememberNavBackStack` persiste/restaure la pile en sérialisant des [NavKey] ; il
 * exige donc un `serializersModule` déclarant le **polymorphisme ouvert** de [NavKey]
 * avec chaque sous-type concret. Sans ça, l'app **plante au démarrage** (le défaut
 * `SavedStateConfiguration.DEFAULT` ne connaît pas nos routes).
 *
 * Toute nouvelle [Route] doit être ajoutée ici via `subclass(...)`.
 * Et son entry de rendu doit être ajoutée dans la fonction `xxxEntries()` de la feature
 * correspondante (cf. `presentation/features/<feature>/<Feature>NavEntries.kt`), elle-même
 * agrégée dans `AppNavDisplay`. Le mapping route→écran est ainsi distribué par feature ;
 * seul l'enregistrement de sérialisation reste centralisé ici (un seul point de vérité).
 */
val appNavConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Splash::class)
            subclass(Route.Login::class)
            subclass(Route.Register::class)
            subclass(Route.Home::class)
            subclass(Route.Settings::class)
            subclass(Route.Campaigns::class)
            subclass(Route.CampaignDetail::class)
            subclass(Route.CharacterSheets::class)
        }
    }
}
