package eu.ejdr.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.SessionStatus
import eu.ejdr.presentation.features.auth.authEntries
import eu.ejdr.presentation.features.campaign.campaignEntries
import eu.ejdr.presentation.features.charactersheet.characterSheetEntries
import eu.ejdr.presentation.features.friendgroup.friendGroupEntries
import eu.ejdr.presentation.features.reference.referenceEntries
import eu.ejdr.presentation.features.session.sessionEntries
import eu.ejdr.presentation.features.settings.settingsEntries
import eu.ejdr.presentation.features.user.userEntries
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Rend l'écran courant à partir du back-stack possédé par [eu.ejdr.presentation.App] (Android).
 *
 * Pendant mobile de l'`AppNavDisplay` desktop : même back-stack Navigation3 et même décorateur de
 * rétention des ViewModels par destination, mais la navigation principale est une **bottom bar**
 * (`NavigationBar`) alimentée par `appNavItems` partagés, au lieu d'une sidebar.
 *
 * Tant que les pages Android par feature ne sont pas implémentées, les destinations sans rendu
 * affichent un écran d'attente ([ComingSoon]). Seul [Route.Splash] est défini ici.
 */
@Composable
fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    sessionStatus: StateFlow<SessionStatus>,
    activeGroupId: StateFlow<String?>,
    onLoggedIn: () -> Unit,
    onLogout: () -> Unit,
    onThemeChange: (ThemeVariant) -> Unit,
    resetTo: (Route) -> Unit,
) {
    val status by sessionStatus.collectAsStateWithLifecycle()
    val groupId by activeGroupId.collectAsStateWithLifecycle()
    val actions = NavActions(backStack, onLoggedIn, onLogout, onThemeChange, resetTo)

    val visibleItems = appNavItems.filter { it.isVisible(status, groupId) }
    val currentRoute = backStack.lastOrNull()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            // Décorateurs : on garde le défaut de NavDisplay
            // (rememberSaveableStateHolderNavEntryDecorator). La rétention des ViewModels
            // par destination (trio Saveable + SavedState + ViewModelStore) sera finalisée
            // plus tard ; sans elle, koinViewModel résout contre le ViewModelStore de
            // l'Activity (partagé) — suffisant pour un écran auth à la fois.
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                // fallback : toute destination encore sans rendu Android affiche ComingSoon
                // (les autres pages Android par feature seront ajoutées ici progressivement).
                entryProvider = entryProvider<Any>(fallback = { key -> NavEntry(key) { ComingSoon(key) } }) {
                    entry<Route.Splash> { SplashScreen() }
                    authEntries(actions)
                    userEntries(actions)
                    friendGroupEntries(actions)
                    settingsEntries(actions)
                    characterSheetEntries(actions)
                    campaignEntries(actions)
                    sessionEntries(actions)
                    referenceEntries(actions)
                },
            )
        }

        if (status == SessionStatus.Authenticated && visibleItems.isNotEmpty()) {
            NavigationBar(Modifier.fillMaxWidth()) {
                visibleItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute?.let { it::class == item.route::class } ?: false,
                        onClick = {
                            if (currentRoute?.let { it::class != item.route::class } != false) {
                                backStack.add(item.route)
                            }
                        },
                        icon = { AppIcon(item.icon, contentDescription = item.label) },
                        // Pas de label : barre plus compacte avec 6 onglets ; le libellé reste
                        // exposé via contentDescription (accessibilité).
                    )
                }
            }
        }
    }
}

/** Écran de démarrage affiché pendant la restauration de session. */
@Composable
private fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.background),
        Alignment.Center,
    ) { CircularProgressIndicator(color = AppTheme.colors.primary) }
}

/** Placeholder temporaire pour une destination dont la page Android n'est pas encore écrite. */
@Composable
private fun ComingSoon(key: Any) {
    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.background),
        Alignment.Center,
    ) {
        AppText(
            text = "Écran Android à venir\n${key::class.simpleName}",
            style = AppTextStyle.Subtitle,
        )
    }
}
