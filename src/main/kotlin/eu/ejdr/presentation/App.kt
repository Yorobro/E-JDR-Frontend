package eu.ejdr.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.rememberNavBackStack
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.navigation.AppNavDisplay
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.navigation.appNavConfiguration
import eu.ejdr.presentation.shared.component.organism.UpdateDialog
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.darkColors
import eu.ejdr.presentation.shared.theme.lightColors
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
 * Naviguer revient à empiler/dépiler une [Route] sur le back-stack ; les ViewModels
 * des écrans sont retenus par destination (cf. [AppNavDisplay]).
 */
@Composable
fun App() {
    val getTheme = koinInject<GetThemeUseCase>()
    var themeVariant by remember { mutableStateOf(ThemeVariant.LIGHT) }
    LaunchedEffect(Unit) { themeVariant = getTheme() }

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

        val backStack = rememberNavBackStack(appNavConfiguration, Route.Splash)
        var updateInfo by remember { mutableStateOf<UpdateInfoDto?>(null) }

        // Remplace toute la pile par une seule destination (ex. après login/logout) :
        // l'historique antérieur ne doit jamais permettre de « revenir » avant l'auth.
        fun resetTo(route: Route) {
            backStack.clear()
            backStack.add(route)
        }

        // Démarrage : vérifie les mises à jour et tente l'auto-login, puis remplace
        // l'écran Splash par Home (succès) ou Login (échec).
        LaunchedEffect(Unit) {
            launch { updateInfo = checkUpdate() }
            resetTo(
                when (restoreSession()) {
                    is Result.Success -> Route.Home
                    is Result.Failure -> Route.Login
                },
            )
        }

        AppNavDisplay(
            backStack = backStack,
            onLogout = { scope.launch { logout(); resetTo(Route.Login) } },
            onThemeChange = { themeVariant = it },
            resetTo = ::resetTo,
        )

        updateInfo?.let { info ->
            UpdateDialog(
                info = info,
                onDismiss = { updateInfo = null },
                downloadAndInstall = downloadAndInstall,
            )
        }
    }
}
