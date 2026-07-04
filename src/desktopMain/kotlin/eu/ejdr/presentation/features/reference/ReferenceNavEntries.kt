package eu.ejdr.presentation.features.reference

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.features.reference.page.ReferenceHubPage
import eu.ejdr.presentation.features.reference.page.ReferenceListPage
import eu.ejdr.presentation.navigation.MainTopBar
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

fun EntryProviderScope<Any>.referenceEntries(actions: NavActions) {
    entry<Route.ReferenceHub> {
        AppScaffold(
            topBar = { MainTopBar(title = "Mes éléments", currentRoute = Route.ReferenceHub, actions = actions) },
        ) {
            ReferenceHubPage(
                onOpenType = { slug -> actions.backStack.add(Route.ReferenceList(slug)) },
            )
        }
    }
    entry<Route.ReferenceList> { key ->
        val type = ReferenceType.fromSlug(key.type)
        AppScaffold(
            topBar = {
                AppTopBar(title = type?.label ?: "Mes éléments", onBack = { actions.backStack.removeLastOrNull() })
            },
        ) {
            if (type != null) ReferenceListPage(type = type)
        }
    }
}
