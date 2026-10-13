package eu.ejdr.presentation.features.session

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.session.page.SessionDetailPage
import eu.ejdr.presentation.features.session.page.SessionGamePage
import eu.ejdr.presentation.features.session.page.SessionLobbyPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entries de navigation de la feature sessions (détail + lobby). */
fun EntryProviderScope<Any>.sessionEntries(actions: NavActions) {
    entry<Route.SessionDetail> { key ->
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = key.title,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            SessionDetailPage(
                id = key.id,
                title = key.title,
                onDeleted = { actions.backStack.removeLastOrNull() },
                onLobbyOpened = { id, title -> actions.backStack.add(Route.SessionLobby(id, title)) },
            )
        }
    }
    entry<Route.SessionLobby> { key ->
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = key.title,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            SessionLobbyPage(
                sessionId = key.id,
                title = key.title,
                // Session démarrée : on remplace le lobby par l'écran de jeu dans la pile.
                onStarted = {
                    actions.backStack.removeLastOrNull()
                    actions.backStack.add(Route.SessionGame(key.id, key.title))
                },
            )
        }
    }
    entry<Route.SessionGame> { key ->
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = key.title,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            SessionGamePage(sessionId = key.id, title = key.title)
        }
    }
}
