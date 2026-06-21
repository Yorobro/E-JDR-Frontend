package eu.ejdr.presentation.features.user

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.user.page.UserPage
import eu.ejdr.presentation.navigation.MainTopBar
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold

fun EntryProviderScope<Any>.userEntries(actions: NavActions) {
    entry<Route.Home> {
        AppScaffold(
            topBar = { MainTopBar(title = "E-JDR", currentRoute = Route.Home, actions = actions) },
        ) {
            UserPage(onSessionExpired = { actions.resetTo(Route.Login) }, onLogout = actions.onLogout)
        }
    }
}
