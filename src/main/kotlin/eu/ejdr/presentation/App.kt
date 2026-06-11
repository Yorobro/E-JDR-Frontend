package eu.ejdr.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import eu.ejdr.application.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.application.settings.abstraction.ThemeVariant
import eu.ejdr.application.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.update.abstraction.UpdateInfo
import eu.ejdr.application.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.presentation.navigation.AppRouter
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Screen
import eu.ejdr.presentation.shared.component.organism.UpdateDialog
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.darkColors
import eu.ejdr.presentation.shared.theme.lightColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable racine de l'application.
 *
 * Charge le thème persisté avant de fournir le design system via [AppTheme], puis orchestre le
 * démarrage (auto-login via [RestoreSessionUseCase], vérification de mise à jour via
 * [CheckUpdateUseCase]) et maintient l'état de navigation ([Screen]). Le rendu de chaque écran
 * est délégué à [AppRouter].
 *
 * Deux zones distinctes :
 * - **non-connectée** (Login / Register) : rendue en plein écran ;
 * - **connectée** (Home / Settings) : rendue dans un [eu.ejdr.presentation.shared.component.organism.AppScaffold]
 *   avec une [eu.ejdr.presentation.shared.component.organism.AppTopBar] présente partout, dont le
 *   bouton Déconnexion appelle [LogoutUseCase] avant de revenir à la connexion.
 */
@Composable
fun App() {
    val getTheme = koinInject<GetThemeUseCase>()
    var themeVariant by remember { mutableStateOf(getTheme()) }

    AppTheme(
        colors = when (themeVariant) {
            ThemeVariant.LIGHT -> lightColors()
            ThemeVariant.DARK -> darkColors()
        },
    ) {
        val restoreSession = koinInject<RestoreSessionUseCase>()
        val logout = koinInject<LogoutUseCase>()
        val checkUpdate = koinInject<CheckUpdateUseCase>()
        val downloadAndInstall = koinInject<DownloadAndInstallUpdateUseCase>()
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

        LaunchedEffect(Unit) {
            launch { updateInfo = checkUpdate() }
            screen = when (val result = restoreSession()) {
                is Result.Success -> Screen.Home(user = result.value)
                is Result.Failure -> Screen.Login
            }
        }

        val actions = NavActions(
            onAuthenticated = { user -> screen = Screen.Home(user) },
            onGoToRegister = { screen = Screen.Register },
            onGoToLogin = { screen = Screen.Login },
            onLogout = { scope.launch { logout(); screen = Screen.Login } },
            onSettings = {
                val current = screen
                if (current is Screen.Home) screen = Screen.Settings(current.user)
            },
            onBack = {
                val current = screen
                if (current is Screen.Settings) screen = Screen.Home(current.user)
            },
            onThemeChange = { themeVariant = it },
            onSessionExpired = { screen = Screen.Login },
        )

        AppRouter(screen = screen, actions = actions)

        updateInfo?.let { info ->
            UpdateDialog(
                info = info,
                onDismiss = { updateInfo = null },
                downloadAndInstall = downloadAndInstall,
            )
        }
    }
}
