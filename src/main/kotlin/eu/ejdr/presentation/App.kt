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
import eu.ejdr.presentation.feature.auth.page.LoginPage
import eu.ejdr.presentation.feature.auth.page.RegisterPage
import eu.ejdr.presentation.feature.user.page.UserPage
import eu.ejdr.presentation.navigation.Screen
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import eu.ejdr.presentation.shared.theme.AppTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable racine de l'application.
 *
 * Fournit le design system via [AppTheme], puis route par état entre les écrans ([Screen]).
 * Au démarrage, tente un auto-login via [RestoreSessionUseCase] : succès → zone connectée
 * ([Screen.User]), échec → connexion ([Screen.Login]).
 *
 * Deux zones distinctes :
 * - **non-connectée** (Login / Register) : rendue en plein écran ;
 * - **connectée** (User) : rendue dans un [AppScaffold] avec une [AppTopBar] présente partout,
 *   dont le bouton Déconnexion appelle [LogoutUseCase] avant de revenir à la connexion.
 */
@Composable
fun App() {
    AppTheme {
        val restoreSession = koinInject<RestoreSessionUseCase>()
        val logout = koinInject<LogoutUseCase>()
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf<Screen>(Screen.Splash) }

        LaunchedEffect(Unit) {
            screen = when (restoreSession()) {
                is Result.Success -> Screen.User(user = null)
                is Result.Failure -> Screen.Login
            }
        }

        when (val current = screen) {
            Screen.Splash -> Box(
                Modifier.fillMaxSize().background(AppTheme.colors.background),
                Alignment.Center,
            ) { CircularProgressIndicator(color = AppTheme.colors.primary) }

            Screen.Login -> LoginPage(
                onAuthenticated = { user -> screen = Screen.User(user) },
                onGoToRegister = { screen = Screen.Register },
            )

            Screen.Register -> RegisterPage(
                onAuthenticated = { user -> screen = Screen.User(user) },
                onGoToLogin = { screen = Screen.Login },
            )

            is Screen.User -> AppScaffold(
                topBar = {
                    AppTopBar(
                        title = "E-JDR",
                        onLogout = {
                            scope.launch {
                                logout()
                                screen = Screen.Login
                            }
                        },
                    )
                },
            ) {
                UserPage(user = current.user)
            }
        }
    }
}
