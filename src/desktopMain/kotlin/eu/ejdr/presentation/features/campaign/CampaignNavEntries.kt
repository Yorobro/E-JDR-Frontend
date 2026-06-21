package eu.ejdr.presentation.features.campaign

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.campaign.page.CampaignDetailPage
import eu.ejdr.presentation.features.campaign.page.CampaignListPage
import eu.ejdr.presentation.navigation.MainTopBar
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

fun EntryProviderScope<Any>.campaignEntries(actions: NavActions) {
    entry<Route.Campaigns> {
        AppScaffold(
            topBar = { MainTopBar(title = "Campagnes", currentRoute = Route.Campaigns, actions = actions) },
        ) {
            CampaignListPage(
                onOpenCampaign = { id, name -> actions.backStack.add(Route.CampaignDetail(id, name)) },
            )
        }
    }
    entry<Route.CampaignDetail> { key ->
        AppScaffold(
            topBar = {
                AppTopBar(title = key.name, onBack = { actions.backStack.removeLastOrNull() })
            },
        ) {
            CampaignDetailPage(
                id = key.id,
                name = key.name,
                onOpenSession = { id, title -> actions.backStack.add(Route.SessionDetail(id, title)) },
            )
        }
    }
}
