package eu.ejdr.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.realtime.RealtimeCoordinator
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.getOrNull
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.update.UpdateController
import eu.ejdr.presentation.navigation.AppNavDisplay
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.navigation.appNavConfiguration
import eu.ejdr.presentation.shared.component.organism.UpdateDialog
import eu.ejdr.presentation.shared.feedback.UiMessageHost
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.colorsFor
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable racine de l'application.
 *
 * Responsabilités : fournir le design system ([AppTheme]), orchestrer le démarrage
 * (auto-login via [RestoreSessionUseCase], vérification de mise à jour via
 * [CheckUpdateUseCase]) et **posséder le back-stack de navigation**
 * ([rememberNavBackStack]). Le mapping route → écran est délégué à [AppNavDisplay].
 *
 * L'état transverse (thème + statut de session) est centralisé dans [RootState], source
 * de vérité unique retenue ici par `remember` et pilotée par un [rememberCoroutineScope].
 * `App` ne fait qu'**observer** ce state et le traduire en navigation (resetTo).
 *
 * Naviguer revient à empiler/dépiler une [Route] sur le back-stack ; les ViewModels
 * des écrans sont retenus par destination (cf. [AppNavDisplay]).
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val getTheme = koinInject<GetThemeUseCase>()
    val restoreSession = koinInject<RestoreSessionUseCase>()
    val realtimeCoordinator = koinInject<RealtimeCoordinator>()
    val rootState = remember { RootState(scope, getTheme, restoreSession, realtimeCoordinator) }
    val themeVariant by rootState.theme.collectAsStateWithLifecycle()

    AppTheme(colors = colorsFor(themeVariant)) {
        val logout = koinInject<LogoutUseCase>()
        val checkUpdate = koinInject<CheckUpdateUseCase>()
        val downloadAndInstall = koinInject<DownloadAndInstallUpdateUseCase>()

        val backStack = rememberNavBackStack(appNavConfiguration, Route.Splash)
        var updateInfo by remember { mutableStateOf<UpdateInfoDto?>(null) }
        val sessionStatus by rootState.sessionStatus.collectAsStateWithLifecycle()

        // Remplace toute la pile par une seule destination (ex. après login/logout) :
        // l'historique antérieur ne doit jamais permettre de « revenir » avant l'auth.
        fun resetTo(route: Route) {
            backStack.clear()
            backStack.add(route)
        }

        // Démarrage : vérifie les mises à jour et lance l'auto-login. Le résultat de la
        // restauration est publié dans rootState.sessionStatus, observé ci-dessous.
        LaunchedEffect(Unit) {
            launch { updateInfo = checkUpdate().getOrNull() }
            rootState.restoreSession()
        }

        // Traduit le statut de session en navigation : remplace l'écran Splash par Home
        // (session restaurée) ou Login (échec). Tant que le statut est Unknown, on attend.
        LaunchedEffect(sessionStatus) {
            when (sessionStatus) {
                SessionStatus.Authenticated -> resetTo(Route.Home)
                SessionStatus.Unauthenticated -> resetTo(Route.Login)
                SessionStatus.Unknown -> Unit
            }
        }

        Box(Modifier.fillMaxSize()) {
            AppNavDisplay(
                backStack = backStack,
                onLoggedIn = rootState::onLoggedIn,
                onLogout = { scope.launch { logout(); rootState.onLoggedOut(); resetTo(Route.Login) } },
                onThemeChange = rootState::setTheme,
                resetTo = ::resetTo,
            )
            UiMessageHost(bus = koinInject())
        }

        updateInfo?.let { info ->
            val updateController = remember { UpdateController(downloadAndInstall, scope) }
            val downloadState by updateController.state.collectAsStateWithLifecycle()
            val startDownload: () -> Unit = {
                info.downloadUrl?.let { url -> updateController.download(url, info.sha256Url) }
            }
            UpdateDialog(
                info = info,
                state = downloadState,
                onInstall = startDownload,
                onRetry = startDownload,
                onOpenReleasePage = {
                    runCatching { Desktop.getDesktop().browse(URI(info.releaseUrl)) }
                    updateInfo = null
                },
                onDismiss = { updateInfo = null },
            )
        }
    }
}
