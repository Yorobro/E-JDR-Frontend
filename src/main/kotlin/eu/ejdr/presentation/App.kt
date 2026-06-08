package eu.ejdr.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.application.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.application.settings.abstraction.ThemeVariant
import eu.ejdr.application.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.update.abstraction.UpdateInfo
import eu.ejdr.application.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.presentation.feature.auth.page.LoginPage
import eu.ejdr.presentation.feature.auth.page.RegisterPage
import eu.ejdr.presentation.feature.settings.page.SettingsPage
import eu.ejdr.presentation.feature.user.page.UserPage
import eu.ejdr.presentation.navigation.Screen
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import eu.ejdr.presentation.shared.component.organism.UpdateDialog
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.darkColors
import eu.ejdr.presentation.shared.theme.lightColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable racine de l'application.
 *
 * Charge le thème persisté avant de fournir le design system via [AppTheme], puis route par état
 * entre les écrans ([Screen]). Au démarrage, tente un auto-login via [RestoreSessionUseCase] :
 * succès → zone connectée ([Screen.Home]), échec → connexion ([Screen.Login]).
 *
 * Deux zones distinctes :
 * - **non-connectée** (Login / Register) : rendue en plein écran ;
 * - **connectée** (Home / Settings) : rendue dans un [AppScaffold] avec une [AppTopBar] présente
 *   partout, dont le bouton Déconnexion appelle [LogoutUseCase] avant de revenir à la connexion.
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

        val onLogout: () -> Unit = { scope.launch { logout(); screen = Screen.Login } }

        LaunchedEffect(Unit) {
            launch { updateInfo = checkUpdate() }
            screen = when (val result = restoreSession()) {
                is Result.Success -> Screen.Home(user = result.value)
                is Result.Failure -> Screen.Login
            }
        }

        when (val current = screen) {
            Screen.Splash -> Box(
                Modifier.fillMaxSize().background(AppTheme.colors.background),
                Alignment.Center,
            ) { CircularProgressIndicator(color = AppTheme.colors.primary) }

            Screen.Login -> LoginPage(
                onAuthenticated = { user -> screen = Screen.Home(user) },
                onGoToRegister = { screen = Screen.Register },
            )

            Screen.Register -> RegisterPage(
                onAuthenticated = { user -> screen = Screen.Home(user) },
                onGoToLogin = { screen = Screen.Login },
            )

            is Screen.Home -> AppScaffold(
                topBar = {
                    AppTopBar(
                        title = "E-JDR",
                        onLogout = onLogout,
                        onSettings = { screen = Screen.Settings(current.user) },
                    )
                },
            ) {
                UserPage(user = current.user)
            }

            is Screen.Settings -> AppScaffold(
                topBar = {
                    AppTopBar(
                        title = "Paramètres",
                        onLogout = onLogout,
                        onBack = { screen = Screen.Home(current.user) },
                    )
                },
            ) {
                SettingsPage(onThemeChange = { themeVariant = it })
            }
        }

        updateInfo?.let { info ->
            UpdateDialog(
                info = info,
                onDismiss = { updateInfo = null },
                downloadAndInstall = downloadAndInstall,
            )
        }
    }
}
