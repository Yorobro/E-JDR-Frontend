package eu.ejdr.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.auth.authEntries
import eu.ejdr.presentation.features.campaign.campaignEntries
import eu.ejdr.presentation.features.friendgroup.friendGroupEntries
import eu.ejdr.presentation.features.session.sessionEntries
import eu.ejdr.presentation.features.reference.referenceEntries
import eu.ejdr.presentation.features.charactersheet.characterSheetEntries
import eu.ejdr.presentation.features.settings.settingsEntries
import eu.ejdr.presentation.features.user.userEntries
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Rend l'écran courant à partir du back-stack possédé par [eu.ejdr.presentation.App]
 * (Navigation 3).
 *
 * Ce composable concentre **uniquement** le mapping route → écran et les transitions
 * de navigation ; l'orchestration du démarrage (auto-login, mise à jour) et l'état du
 * thème restent dans [eu.ejdr.presentation.App]. Le décorateur
 * [rememberEjdrViewModelStoreNavEntryDecorator] retient un ViewModel par destination.
 *
 * Le mapping route → écran est **distribué par feature** : chaque feature expose une
 * extension `xxxEntries(actions)` sur le builder d'entries, agrégée ici. Seul l'écran de
 * démarrage ([Route.Splash]), transverse, reste défini inline.
 *
 * @param backStack Pile de navigation à afficher (possédée par l'appelant). Typée
 * [NavKey] car `rememberNavBackStack` produit un `NavBackStack<NavKey>` ; les valeurs
 * empilées sont des [Route] (qui implémentent [NavKey]).
 * @param onLogout Déconnexion (appel use case + retour à l'écran de connexion), déléguée à l'appelant.
 * @param onThemeChange Propage le thème choisi vers [eu.ejdr.presentation.App] pour recomposer le design system.
 * @param resetTo Remplace toute la pile par une destination unique (post-login/logout).
 */
@Composable
fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    onLoggedIn: () -> Unit,
    onLogout: () -> Unit,
    onThemeChange: (ThemeVariant) -> Unit,
    resetTo: (Route) -> Unit,
) {
    val actions = NavActions(backStack, onLoggedIn, onLogout, onThemeChange, resetTo)
    // Transition d'écran : fondu pur (sans glissement). Le glissement faisait apparaître le fond
    // pendant le chevauchement ; le fondu, combiné au fond du thème peint sur le conteneur racine
    // (cf. App), évite tout flash. Durée/courbe pilotées par les tokens, neutralisées en
    // reduced-motion (durée 0 → changement instantané).
    val motion = AppTheme.motion
    val fadeSpec = tween<Float>(
        durationMillis = motion.effectiveDuration(motion.durationMedium),
        easing = motion.easeEmphasized,
    )
    val fadeTransition = fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(rememberEjdrViewModelStoreNavEntryDecorator()),
        transitionSpec = { fadeTransition },
        popTransitionSpec = { fadeTransition },
        entryProvider = entryProvider {
            entry<Route.Splash> { SplashScreen() }
            authEntries(actions)
            userEntries(actions)
            settingsEntries(actions)
            campaignEntries(actions)
            sessionEntries(actions)
            referenceEntries(actions)
            characterSheetEntries(actions)
            friendGroupEntries(actions)
        },
    )
}

/** Écran de démarrage affiché pendant la restauration de session. */
@Composable
private fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.background),
        Alignment.Center,
    ) { CircularProgressIndicator(color = AppTheme.colors.primary) }
}
