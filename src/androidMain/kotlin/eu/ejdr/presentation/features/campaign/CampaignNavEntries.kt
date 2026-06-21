package eu.ejdr.presentation.features.campaign

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.campaign.page.CampaignDetailPage
import eu.ejdr.presentation.features.campaign.page.CampaignListPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/**
 * Entries de navigation des campagnes (Android).
 *
 * [Route.Campaigns] : onglet de 1er niveau (liste). [Route.CampaignDetail] : sous-écran
 * (fiches + sessions) avec [AppTopBar] et retour ; un clic sur une session ouvre son détail.
 */
fun EntryProviderScope<Any>.campaignEntries(actions: NavActions) {
    entry<Route.Campaigns> {
        CampaignListPage(
            onOpenCampaign = { id, name -> actions.backStack.add(Route.CampaignDetail(id, name)) },
        )
    }

    entry<Route.CampaignDetail> { key ->
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = key.name, onBack = { actions.backStack.removeLastOrNull() })
            CampaignDetailPage(
                id = key.id,
                name = key.name,
                onOpenSession = { id, title -> actions.backStack.add(Route.SessionDetail(id, title)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
