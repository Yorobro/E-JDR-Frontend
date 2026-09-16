package eu.ejdr.presentation.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import eu.ejdr.presentation.SessionStatus
import eu.ejdr.presentation.shared.icons.AppIcons

data class NavItem(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    val isVisible: (sessionStatus: SessionStatus, activeGroupId: String?) -> Boolean,
)

val appNavItems: List<NavItem> = listOf(
    NavItem(
        route = Route.Home,
        label = "Accueil",
        icon = AppIcons.Home,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Campaigns,
        label = "Campagnes",
        icon = AppIcons.Castle,
        isVisible = { s, g -> s == SessionStatus.Authenticated && g != null },
    ),
    NavItem(
        route = Route.CharacterSheets,
        label = "Fiches",
        icon = AppIcons.Badge,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.ReferenceHub,
        label = "Références",
        icon = AppIcons.MenuBook,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Groups,
        label = "Groupes",
        icon = AppIcons.Groups,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Settings,
        label = "Paramètres",
        icon = AppIcons.Settings,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
)
