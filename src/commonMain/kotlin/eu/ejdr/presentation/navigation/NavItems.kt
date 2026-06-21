package eu.ejdr.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import eu.ejdr.presentation.SessionStatus

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
        icon = Icons.Default.Home,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Campaigns,
        label = "Campagnes",
        icon = Icons.Default.Castle,
        isVisible = { s, g -> s == SessionStatus.Authenticated && g != null },
    ),
    NavItem(
        route = Route.CharacterSheets,
        label = "Fiches",
        icon = Icons.Default.Badge,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.ReferenceHub,
        label = "Références",
        icon = Icons.Default.MenuBook,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Groups,
        label = "Groupes",
        icon = Icons.Default.Groups,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Settings,
        label = "Paramètres",
        icon = Icons.Default.Settings,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
)
