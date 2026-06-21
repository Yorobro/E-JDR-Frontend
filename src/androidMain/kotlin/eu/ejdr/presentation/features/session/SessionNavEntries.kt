package eu.ejdr.presentation.features.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.session.page.SessionDetailPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entry de navigation du détail de session (Android) : sous-écran avec AppTopBar et retour. */
fun EntryProviderScope<Any>.sessionEntries(actions: NavActions) {
    entry<Route.SessionDetail> { key ->
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = key.title, onBack = { actions.backStack.removeLastOrNull() })
            SessionDetailPage(
                id = key.id,
                title = key.title,
                onDeleted = { actions.backStack.removeLastOrNull() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
