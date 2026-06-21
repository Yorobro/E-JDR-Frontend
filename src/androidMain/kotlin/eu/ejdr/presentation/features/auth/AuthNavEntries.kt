package eu.ejdr.presentation.features.auth

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.auth.page.LoginPage
import eu.ejdr.presentation.features.auth.page.RegisterPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route

/** Entries de navigation de la feature authentification (Login, Register) — Android. */
fun EntryProviderScope<Any>.authEntries(actions: NavActions) {
    entry<Route.Login> {
        LoginPage(
            onAuthenticated = { actions.onLoggedIn() },
            onGoToRegister = { actions.backStack.add(Route.Register) },
        )
    }
    entry<Route.Register> {
        RegisterPage(
            onAuthenticated = { actions.onLoggedIn() },
            onGoToLogin = { actions.backStack.removeLastOrNull() },
        )
    }
}
