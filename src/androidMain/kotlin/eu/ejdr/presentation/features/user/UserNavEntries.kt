package eu.ejdr.presentation.features.user

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.user.page.UserPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route

/**
 * Entry de navigation de l'écran d'accueil/profil (Android).
 *
 * Pas de top-bar : la navigation principale mobile est la bottom bar gérée par AppNavDisplay.
 */
fun EntryProviderScope<Any>.userEntries(actions: NavActions) {
    entry<Route.Home> {
        UserPage(
            onSessionExpired = { actions.resetTo(Route.Login) },
            onLogout = actions.onLogout,
        )
    }
}
