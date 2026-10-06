package eu.ejdr.presentation.features.session

import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.shared.feedback.UiMessage
import eu.ejdr.application.shared.feedback.UiMessagePlacement
import eu.ejdr.application.shared.feedback.UiMessageBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Ressource poussée par le serveur au joueur qu'un MJ vient de retirer d'une session. */
private const val RESOURCE_SESSION_REMOVED = "session-removed"

/** Message affiché au joueur retiré, où qu'il se trouve dans l'application. */
private const val REMOVED_MESSAGE = "Vous avez été retiré de la session"

/**
 * Veille **globale** sur le retrait du joueur courant d'une session.
 *
 * Vit à la racine de l'application (comme [eu.ejdr.presentation.RootState]) et non dans l'écran
 * du salon d'attente : le joueur doit être averti **où qu'il soit**, y compris s'il n'a jamais
 * ouvert le lobby. C'est aussi ce qui évite de dupliquer la logique entre les écrans.
 *
 * Le signal arrive sur le canal personnel du joueur (`user:{id}`, auquel le serveur l'abonne dès
 * le handshake WebSocket) : le recevoir suffit à savoir qu'il s'agit de soi — inutile de lire
 * l'utilisateur courant pour se comparer à la liste des participants.
 *
 * Responsabilité bornée : avertir (message en haut de l'écran, il n'a rien demandé), vider l'état
 * du salon et lever [ejected]. **C'est l'appelant qui décide** de la navigation, car lui seul sait
 * sur quel écran se trouve le joueur : sortir du salon d'attente n'a de sens que s'il y est.
 *
 * Vider [SessionLobbyState] fait partie du retrait lui-même, pas de la navigation : le raccourci
 * qui alimente la bulle de retour globale y vit, et il doit disparaître **dans la même
 * recomposition** que l'affichage du message — sinon le joueur garde sous les yeux un bouton vers un
 * salon dont il ne fait plus partie.
 *
 * @property scope Portée de coroutine qui porte l'écoute (celle de la racine).
 * @property invalidationBus Bus d'invalidation temps réel.
 * @property uiMessageBus Bus des messages UI transitoires.
 * @property lobbyState État partagé du salon, vidé au retrait.
 */
class SessionRemovalWatcher(
    scope: CoroutineScope,
    private val invalidationBus: InvalidationBus,
    private val uiMessageBus: UiMessageBus,
    private val lobbyState: SessionLobbyState,
) {

    private val _ejected = MutableStateFlow(false)

    /** Passe à `true` quand le joueur courant vient d'être retiré d'une session. */
    val ejected: StateFlow<Boolean> = _ejected.asStateFlow()

    init {
        scope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource == RESOURCE_SESSION_REMOVED) {
                    lobbyState.clear()
                    uiMessageBus.emit(UiMessage.error(REMOVED_MESSAGE, UiMessagePlacement.TOP))
                    _ejected.value = true
                }
            }
        }
    }

    /** Acquitte le retrait une fois traité (évite de re-naviguer à la recomposition suivante). */
    fun consume() {
        _ejected.value = false
    }
}
