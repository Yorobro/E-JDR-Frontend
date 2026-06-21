package eu.ejdr.presentation.features.reference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.features.reference.page.ReferenceHubPage
import eu.ejdr.presentation.features.reference.page.ReferenceListPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/**
 * Entries de navigation des références (Android).
 *
 * [Route.ReferenceHub] : onglet de 1er niveau (grille des catégories). [Route.ReferenceList] :
 * sous-écran de gestion d'une catégorie (résolue depuis le slug) avec [AppTopBar] et retour.
 */
fun EntryProviderScope<Any>.referenceEntries(actions: NavActions) {
    entry<Route.ReferenceHub> {
        ReferenceHubPage(
            onOpenType = { slug -> actions.backStack.add(Route.ReferenceList(slug)) },
        )
    }

    entry<Route.ReferenceList> { key ->
        val type = ReferenceType.fromSlug(key.type)
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = type?.label ?: "Mes éléments", onBack = { actions.backStack.removeLastOrNull() })
            if (type != null) ReferenceListPage(type = type, modifier = Modifier.weight(1f))
        }
    }
}
