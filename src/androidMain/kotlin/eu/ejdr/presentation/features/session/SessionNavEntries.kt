package eu.ejdr.presentation.features.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.session.page.SessionDetailPage
import eu.ejdr.presentation.features.session.page.SessionGamePage
import eu.ejdr.presentation.features.session.page.SessionLobbyPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entries de navigation de la feature sessions (Android) : détail + lobby, sous-écrans avec retour. */
fun EntryProviderScope<Any>.sessionEntries(actions: NavActions) {
    entry<Route.SessionDetail> { key ->
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = key.title, onBack = { actions.backStack.removeLastOrNull() })
            SessionDetailPage(
                id = key.id,
                title = key.title,
                onDeleted = { actions.backStack.removeLastOrNull() },
                onLobbyOpened = { id, title -> actions.backStack.add(Route.SessionLobby(id, title)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    entry<Route.SessionLobby> { key ->
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = key.title, onBack = { actions.backStack.removeLastOrNull() })
            SessionLobbyPage(
                sessionId = key.id,
                title = key.title,
                // Session démarrée : on remplace le lobby par l'écran de jeu dans la pile.
                onStarted = {
                    actions.backStack.removeLastOrNull()
                    actions.backStack.add(Route.SessionGame(key.id, key.title))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
    entry<Route.SessionGame> { key ->
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = key.title, onBack = { actions.backStack.removeLastOrNull() })
            SessionGamePage(sessionId = key.id, title = key.title, modifier = Modifier.weight(1f))
        }
    }
}
