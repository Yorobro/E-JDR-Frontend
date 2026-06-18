package eu.ejdr.presentation.features.reference

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.features.reference.page.ReferenceHubPage
import eu.ejdr.presentation.features.reference.page.ReferenceListPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entries de navigation de la feature « Mes éléments » (hub + liste générique par type). */
fun EntryProviderScope<Any>.referenceEntries(actions: NavActions) {
    entry<Route.ReferenceHub> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Mes éléments",
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
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
                AppTopBar(
                    title = type?.label ?: "Mes éléments",
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            // Type inconnu (clé corrompue) : on ne rend rien d'autre que le scaffold ; l'utilisateur
            // revient via le bouton retour. Cas théorique (les slugs viennent de l'enum).
            if (type != null) {
                ReferenceListPage(type = type)
            }
        }
    }
}
