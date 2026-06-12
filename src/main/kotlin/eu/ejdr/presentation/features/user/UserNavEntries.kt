package eu.ejdr.presentation.features.user

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.user.page.UserPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entry de navigation de la zone connectée (écran d'accueil). */
fun EntryProviderScope<Any>.userEntries(actions: NavActions) {
    entry<Route.Home> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "E-JDR",
                    onLogout = actions.onLogout,
                    onSettings = { actions.backStack.add(Route.Settings) },
                )
            },
        ) {
            UserPage(onSessionExpired = { actions.resetTo(Route.Login) })
        }
    }
}
