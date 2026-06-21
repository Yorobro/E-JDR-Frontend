package eu.ejdr.presentation.features.friendgroup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.friendgroup.page.GroupDetailPage
import eu.ejdr.presentation.features.friendgroup.page.GroupListPage
import eu.ejdr.presentation.features.friendgroup.page.InvitationsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/**
 * Entries de navigation des groupes (Android).
 *
 * Groups et Invitations sont des destinations de premier niveau (bottom bar) : pas de top-bar.
 * GroupDetail est un sous-écran : on lui ajoute un [AppTopBar] avec un bouton retour, car la
 * bottom bar ne gère pas le retour depuis un détail.
 */
fun EntryProviderScope<Any>.friendGroupEntries(actions: NavActions) {
    entry<Route.Groups> {
        GroupListPage(
            onNavigateToDetail = { group -> actions.backStack.add(Route.GroupDetail(group.id, group.name)) },
            onNavigateToInvitations = { actions.backStack.add(Route.Invitations) },
        )
    }

    entry<Route.GroupDetail> { key ->
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = key.name, onBack = { actions.backStack.removeLastOrNull() })
            GroupDetailPage(groupId = key.id, modifier = Modifier.weight(1f))
        }
    }

    entry<Route.Invitations> {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = "Invitations", onBack = { actions.backStack.removeLastOrNull() })
            InvitationsPage(modifier = Modifier.weight(1f))
        }
    }
}
