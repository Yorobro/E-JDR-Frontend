package eu.ejdr.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.base.AppSpinner
import eu.ejdr.presentation.shared.component.organism.AppBottomBar
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
 *
 * @param contentOverlay Calque libre posé **au-dessus de l'écran courant mais sous la bottom
 * bar**, dans la zone de contenu. C'est ce qui donne à un élément flottant (la bulle de retour au
 * salon) son dégagement au-dessus de la barre sans coder sa hauteur en dur : la mise en page s'en
 * charge. Défaut : rien.
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
    contentOverlay: @Composable () -> Unit = {},
) {
    val status by sessionStatus.collectAsStateWithLifecycle()
    val groupId by activeGroupId.collectAsStateWithLifecycle()
    val actions = NavActions(backStack, onLoggedIn, onLogout, onThemeChange, resetTo)

    val visibleItems = appNavItems.filter { it.isVisible(status, groupId) }
    val currentRoute = backStack.lastOrNull()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            // Décorateurs : fournir cette liste **remplace** celle par défaut de NavDisplay, d'où
            // le `SaveableStateHolder` re-déclaré ici — sans lui, l'état `rememberSaveable` et la
            // position de défilement des pages seraient perdus à chaque aller-retour.
            //
            // `rememberViewModelStoreNavEntryDecorator` donne un ViewModelStore **par destination**,
            // vidé quand l'entrée quitte la pile. Sans lui, `koinViewModel` résolvait contre le
            // ViewModelStore de l'Activity : un seul exemplaire par classe pour toute
            // l'application, jamais `onCleared()`. Deux dégâts concrets — un
            // `SessionLobbyViewModel` quitté restait abonné à `group:{id}` et continuait d'écrire
            // dans l'état partagé du salon (la bulle de retour s'effaçait toute seule quand le MJ
            // démarrait la partie) ; et deux campagnes ouvertes l'une après l'autre partageaient
            // le ViewModel resté sur la première. Le pendant desktop obtient la même chose avec un
            // décorateur maison, le décorateur officiel n'étant pas publié pour cette cible.
            // Changement de page instantané : aucune transition (pas de fondu ni de glissement).
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
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
            contentOverlay()
        }

        if (status == SessionStatus.Authenticated && visibleItems.isNotEmpty()) {
            AppBottomBar(
                items = visibleItems,
                currentRoute = currentRoute,
                onSelect = { route ->
                    if (currentRoute?.let { it::class != route::class } != false) {
                        backStack.add(route)
                    }
                },
            )
        }
    }
}

/** Écran de démarrage affiché pendant la restauration de session. */
@Composable
private fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.background),
        Alignment.Center,
    ) { AppSpinner() }
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
