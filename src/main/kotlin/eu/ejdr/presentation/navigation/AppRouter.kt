package eu.ejdr.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.feature.auth.page.LoginPage
import eu.ejdr.presentation.feature.auth.page.RegisterPage
import eu.ejdr.presentation.feature.settings.page.SettingsPage
import eu.ejdr.presentation.feature.user.page.UserPage
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Routeur de l'application : rend l'écran correspondant à l'état [screen] courant.
 *
 * Ce composable est délibérément **sans état** : il reçoit l'état de navigation depuis
 * [eu.ejdr.presentation.App] et délègue chaque transition au [NavActions] fourni. Toute la
 * logique d'orchestration (auto-login, vérification de mise à jour, gestion du thème) reste dans
 * [eu.ejdr.presentation.App] ; ce composable ne fait que rendre le bon écran.
 *
 * ### Zones de rendu
 * - **Non-connectée** ([Screen.Splash], [Screen.Login], [Screen.Register]) : rendue en plein
 *   écran, sans top bar.
 * - **Connectée** ([Screen.Home], [Screen.Settings]) : rendue dans un [AppScaffold] avec une
 *   [AppTopBar] présente sur chaque écran de la zone.
 *
 * @param screen  État de navigation courant.
 * @param actions Ensemble des callbacks de navigation (voir [NavActions]).
 */
@Composable
fun AppRouter(
    screen: Screen,
    actions: NavActions,
) {
    when (val current = screen) {
        Screen.Splash -> Box(
            Modifier.fillMaxSize().background(AppTheme.colors.background),
            Alignment.Center,
        ) { CircularProgressIndicator(color = AppTheme.colors.primary) }

        Screen.Login -> LoginPage(
            onAuthenticated = actions.onAuthenticated,
            onGoToRegister = actions.onGoToRegister,
        )

        Screen.Register -> RegisterPage(
            onAuthenticated = actions.onAuthenticated,
            onGoToLogin = actions.onGoToLogin,
        )

        is Screen.Home -> AppScaffold(
            topBar = {
                AppTopBar(
                    title = "E-JDR",
                    onLogout = actions.onLogout,
                    onSettings = actions.onSettings,
                )
            },
        ) {
            UserPage(
                user = current.user,
                onSessionExpired = actions.onSessionExpired,
            )
        }

        is Screen.Settings -> AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Paramètres",
                    onLogout = actions.onLogout,
                    onBack = actions.onBack,
                )
            },
        ) {
            SettingsPage(onThemeChange = actions.onThemeChange)
        }
    }
}
