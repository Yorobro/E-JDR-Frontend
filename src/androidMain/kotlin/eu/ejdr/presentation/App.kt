package eu.ejdr.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.navigation.AppNavDisplay
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.navigation.appNavConfiguration
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.darkColors
import eu.ejdr.presentation.shared.theme.lightColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable racine de l'application **Android**.
 *
 * Pendant mobile de l'`App` desktop : mêmes responsabilités (design system [AppTheme],
 * orchestration du démarrage via [RestoreSessionUseCase], possession du back-stack
 * Navigation3) et même état transverse centralisé dans [RootState]. La différence est le
 * rendu (bottom bar dans [AppNavDisplay]) et la source du groupe actif, ici partagée via
 * [ActiveGroupState] (commun) injecté par Koin.
 *
 * Note : la prompt de mise à jour desktop (`UpdateDialog` + téléchargement/installation) est
 * spécifique au desktop ; sur Android la mise à jour passera par le Play Store (à brancher dans
 * une tâche ultérieure). Ce shell vérifie la session et affiche la navigation principale.
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val getTheme = koinInject<GetThemeUseCase>()
    val restoreSession = koinInject<RestoreSessionUseCase>()
    val rootState = remember { RootState(scope, getTheme, restoreSession) }
    val themeVariant by rootState.theme.collectAsStateWithLifecycle()

    AppTheme(
        colors = when (themeVariant) {
            ThemeVariant.LIGHT -> lightColors()
            ThemeVariant.DARK -> darkColors()
        },
    ) {
        val logout = koinInject<LogoutUseCase>()
        val activeGroupState = koinInject<ActiveGroupState>()

        val backStack = rememberNavBackStack(appNavConfiguration, Route.Splash)
        val sessionStatus by rootState.sessionStatus.collectAsStateWithLifecycle()

        // Remplace toute la pile par une seule destination (post-login/logout) : l'historique
        // antérieur ne doit jamais permettre de « revenir » avant l'authentification.
        fun resetTo(route: Route) {
            backStack.clear()
            backStack.add(route)
        }

        LaunchedEffect(Unit) { rootState.restoreSession() }

        LaunchedEffect(sessionStatus) {
            when (sessionStatus) {
                SessionStatus.Authenticated -> resetTo(Route.Home)
                SessionStatus.Unauthenticated -> resetTo(Route.Login)
                SessionStatus.Unknown -> Unit
            }
        }

        // Fond global de l'app : sans ce Surface, le conteneur racine resterait sur le blanc
        // par défaut de la fenêtre (seules les cartes étaient colorées) → fond clair persistant
        // en thème sombre. On utilise la couleur de fond du thème.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppTheme.colors.background,
        ) {
            AppNavDisplay(
                backStack = backStack,
                sessionStatus = rootState.sessionStatus,
                activeGroupId = activeGroupState.activeGroupId,
                onLogout = { scope.launch { logout(); resetTo(Route.Login) } },
                onThemeChange = rootState::setTheme,
                resetTo = ::resetTo,
            )
        }
    }
}
