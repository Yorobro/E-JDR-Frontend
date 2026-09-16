package eu.ejdr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.shared.component.organism.AppTooltip
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import org.koin.compose.koinInject

/**
 * Top bar partagée par toutes les pages principales (profil, groupes, invitations,
 * campagnes, fiches, éléments, paramètres).
 *
 * La navigation entre ces pages utilise [NavActions.resetTo] (pile plate, pas de back).
 * Les boutons workspace (campagnes/fiches/éléments) sont masqués sans groupe actif.
 *
 * @param title Titre affiché à gauche.
 * @param currentRoute Route active, pour indiquer l'onglet courant (icône profil en couleur).
 * @param actions Actions de navigation.
 */
@Composable
fun MainTopBar(title: String, currentRoute: Route, actions: NavActions) {
    val activeGroupState = koinInject<ActiveGroupState>()
    val activeGroupId by activeGroupState.activeGroupId.collectAsStateWithLifecycle()

    val hasGroup = activeGroupId != null

    AppTopBar(
        title = title,
        onProfile = { actions.resetTo(Route.Home) },
        profileActive = currentRoute == Route.Home,
        onCampaigns = if (hasGroup) { { actions.resetTo(Route.Campaigns) } } else null,
        onCharacterSheets = if (hasGroup) { { actions.resetTo(Route.CharacterSheets) } } else null,
        onReferences = if (hasGroup) { { actions.resetTo(Route.ReferenceHub) } } else null,
        onGroups = { actions.resetTo(Route.Groups) },
        onInvitations = { actions.resetTo(Route.Invitations) },
        onSettings = { actions.resetTo(Route.Settings) },
        // Desktop : chaque icône seule est nommée par une bulle au survol.
        wrapAction = { label, content -> AppTooltip(label) { content() } },
    )
}
