package eu.ejdr.presentation.features.session

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.session.page.SessionDetailPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entries de navigation de la feature sessions (détail). */
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
            )
        }
    }
}
