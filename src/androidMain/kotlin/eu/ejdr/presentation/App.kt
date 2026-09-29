package eu.ejdr.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import eu.ejdr.application.features.realtime.RealtimeCoordinator
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.features.session.SessionRemovalWatcher
import eu.ejdr.presentation.features.session.SessionStatusWatcher
import eu.ejdr.presentation.features.session.component.SessionReturnBubble
import eu.ejdr.presentation.navigation.AppNavDisplay
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.navigation.appNavConfiguration
import eu.ejdr.presentation.navigation.openSession
import eu.ejdr.presentation.shared.feedback.UiMessageHost
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.colorsFor
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
    val realtimeCoordinator = koinInject<RealtimeCoordinator>()
    val rootState = remember { RootState(scope, getTheme, restoreSession, realtimeCoordinator) }
    val themeVariant by rootState.theme.collectAsStateWithLifecycle()

    AppTheme(colors = colorsFor(themeVariant)) {
        val logout = koinInject<LogoutUseCase>()
        val activeGroupState = koinInject<ActiveGroupState>()

        val backStack = rememberNavBackStack(appNavConfiguration, Route.Splash)
        val invalidationBus = koinInject<InvalidationBus>()
        val uiMessageBus = koinInject<UiMessageBus>()
        val lobbyState = koinInject<SessionLobbyState>()
        val sessionStatus by rootState.sessionStatus.collectAsStateWithLifecycle()

        // Remplace toute la pile par une seule destination (post-login/logout) : l'historique
        // antérieur ne doit jamais permettre de « revenir » avant l'authentification.
        fun resetTo(route: Route) {
            backStack.clear()
            backStack.add(route)
        }

        LaunchedEffect(Unit) { rootState.restoreSession() }

        // Veille globale : un joueur retiré d'une session doit être averti même s'il n'a jamais
        // ouvert le salon d'attente. D'où une écoute à la racine plutôt que dans l'écran du lobby.
        val removalWatcher =
            remember { SessionRemovalWatcher(scope, invalidationBus, uiMessageBus, lobbyState) }

        // Même raison d'être à la racine : le démarrage de la partie doit être pris en compte
        // même si l'utilisateur a quitté la page du salon — c'est justement lui qui a besoin
        // que son raccourci bascule de « Retour au salon » à « Rejoindre la partie ». Le
        // ViewModel du salon ne peut pas s'en charger : il meurt avec la page.
        val getSessionLobby = koinInject<GetSessionLobbyUseCase>()
        remember { SessionStatusWatcher(scope, invalidationBus, lobbyState, getSessionLobby) }

        LaunchedEffect(sessionStatus) {
            when (sessionStatus) {
                SessionStatus.Authenticated -> resetTo(Route.Home)
                SessionStatus.Unauthenticated -> resetTo(Route.Login)
                SessionStatus.Unknown -> Unit
            }
        }

        // Joueur retiré : le watcher a déjà affiché le message. S'il était encore dans le salon
        // d'attente, on l'en sort — pile vidée plutôt que dépilée, pour qu'un retour arrière ne le
        // ramène pas dans un lobby dont il ne fait plus partie.
        val ejectedFromSession by removalWatcher.ejected.collectAsStateWithLifecycle()
        LaunchedEffect(ejectedFromSession) {
            if (ejectedFromSession) {
                if (backStack.lastOrNull() is Route.SessionLobby) resetTo(Route.Home)
                removalWatcher.consume()
            }
        }

        // Route au sommet de la pile, lue **en composition** : la bulle de retour au salon doit
        // se masquer dès qu'on est déjà dans la session. Le back-stack est une liste observable,
        // donc la lecture suffit à déclencher la recomposition.
        val currentRoute = backStack.lastOrNull()

        // Déjà dans la session (salon ou partie) : la bulle n'a rien à proposer.
        val alreadyInSession =
            currentRoute is Route.SessionLobby || currentRoute is Route.SessionGame

        // Fond global de l'app : sans ce fond, le conteneur racine resterait sur le blanc
        // par défaut de la fenêtre (seules les cartes étaient colorées) → fond clair persistant
        // en thème sombre. On utilise la couleur de fond du thème.
        Box(
            modifier = Modifier.fillMaxSize().background(AppTheme.colors.background),
        ) {
            AppNavDisplay(
                backStack = backStack,
                sessionStatus = rootState.sessionStatus,
                activeGroupId = activeGroupState.activeGroupId,
                onLoggedIn = rootState::onLoggedIn,
                // `clear()` au logout : sans ça, le compte suivant sur le même appareil hériterait
                // d'une bulle vers un salon qui n'est pas le sien.
                onLogout = {
                    scope.launch {
                        logout()
                        lobbyState.clear()
                        rootState.onLoggedOut()
                        resetTo(Route.Login)
                    }
                },
                onThemeChange = rootState::setTheme,
                resetTo = ::resetTo,
                // Bulle de retour à la session : on peut quitter la page du salon — puis celle de
                // la partie — tout en y restant engagé côté serveur. Posée dans la zone de
                // contenu, donc au-dessus de l'AppBottomBar qu'elle recouvrirait si elle était
                // montée à la racine.
                contentOverlay = {
                    SessionReturnBubble(
                        lobbyState = lobbyState,
                        alreadyInSession = alreadyInSession,
                        onOpenSession = { shortcut -> backStack.openSession(shortcut) },
                    )
                },
            )
            UiMessageHost(bus = uiMessageBus)
        }
    }
}
