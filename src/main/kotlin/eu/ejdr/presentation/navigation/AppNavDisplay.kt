package eu.ejdr.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.presentation.features.auth.page.LoginPage
import eu.ejdr.presentation.features.auth.page.RegisterPage
import eu.ejdr.presentation.features.settings.page.SettingsPage
import eu.ejdr.presentation.features.user.page.UserPage
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Rend l'écran courant à partir du back-stack possédé par [eu.ejdr.presentation.App]
 * (Navigation 3).
 *
 * Ce composable concentre **uniquement** le mapping route → écran et les transitions
 * de navigation ; l'orchestration du démarrage (auto-login, mise à jour) et l'état du
 * thème restent dans [eu.ejdr.presentation.App]. Le décorateur
 * [rememberViewModelStoreNavEntryDecorator] retient un ViewModel par destination.
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
    onLogout: () -> Unit,
    onThemeChange: (ThemeVariant) -> Unit,
    resetTo: (Route) -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(rememberViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<Route.Splash> { SplashScreen() }

            entry<Route.Login> {
                LoginPage(
                    onAuthenticated = { resetTo(Route.Home) },
                    onGoToRegister = { backStack.add(Route.Register) },
                )
            }

            entry<Route.Register> {
                RegisterPage(
                    onAuthenticated = { resetTo(Route.Home) },
                    onGoToLogin = { backStack.removeLastOrNull() },
                )
            }

            entry<Route.Home> {
                AppScaffold(
                    topBar = {
                        AppTopBar(
                            title = "E-JDR",
                            onLogout = onLogout,
                            onSettings = { backStack.add(Route.Settings) },
                        )
                    },
                ) {
                    UserPage(onSessionExpired = { resetTo(Route.Login) })
                }
            }

            entry<Route.Settings> {
                AppScaffold(
                    topBar = {
                        AppTopBar(
                            title = "Paramètres",
                            onLogout = onLogout,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    },
                ) {
                    SettingsPage(onThemeChange = onThemeChange)
                }
            }
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
