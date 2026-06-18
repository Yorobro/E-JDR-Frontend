package eu.ejdr.presentation.features.friendgroup

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.friendgroup.page.GroupDetailPage
import eu.ejdr.presentation.features.friendgroup.page.GroupListPage
import eu.ejdr.presentation.features.friendgroup.page.InvitationsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

fun EntryProviderScope<Any>.friendGroupEntries(actions: NavActions) {
    entry<Route.Groups> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Mes groupes",
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            GroupListPage(
                onNavigateToDetail = { group ->
                    actions.backStack.add(Route.GroupDetail(group.id, group.name))
                },
            )
        }
    }

    entry<Route.GroupDetail> { key ->
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = key.name,
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            GroupDetailPage(groupId = key.id)
        }
    }

    entry<Route.Invitations> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Invitations",
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            InvitationsPage()
        }
    }
}
