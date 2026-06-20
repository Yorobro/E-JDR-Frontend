package eu.ejdr.presentation.features.user

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.user.page.UserPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import org.koin.compose.koinInject

/** Entry de navigation de la zone connectée (écran d'accueil). */
fun EntryProviderScope<Any>.userEntries(actions: NavActions) {
    entry<Route.Home> {
        val activeGroupState = koinInject<ActiveGroupState>()
        val activeGroupId by activeGroupState.activeGroupId.collectAsStateWithLifecycle()
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "E-JDR",
                    onCampaigns = activeGroupId?.let { { actions.backStack.add(Route.Campaigns) } },
                    onCharacterSheets = activeGroupId?.let { { actions.backStack.add(Route.CharacterSheets) } },
                    onReferences = activeGroupId?.let { { actions.backStack.add(Route.ReferenceHub) } },
                    onGroups = { actions.backStack.add(Route.Groups) },
                    onInvitations = { actions.backStack.add(Route.Invitations) },
                    onSettings = { actions.backStack.add(Route.Settings) },
                )
            },
        ) {
            UserPage(onSessionExpired = { actions.resetTo(Route.Login) }, onLogout = actions.onLogout)
        }
    }
}
