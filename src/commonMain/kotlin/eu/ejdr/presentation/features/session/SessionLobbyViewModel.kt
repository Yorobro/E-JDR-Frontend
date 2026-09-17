package eu.ejdr.presentation.features.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.InviteToLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.StartSessionUseCase
import eu.ejdr.application.shared.feedback.UiMessage
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Statut d'une session dont la partie a réellement démarré (renvoyé par le serveur). */
private const val SESSION_STATUS_ACTIVE = "ACTIVE"

/**
 * ViewModel du salon d'attente : rend le lobby **réactif en temps réel** et pilote le démarrage.
 *
 * Le rendu lit toujours l'état partagé [SessionLobbyState] (alimenté à l'ouverture par le MJ ou
 * à l'acceptation par un joueur). Ce ViewModel s'abonne au canal du groupe (`group:{id}`) et :
 * - à chaque invalidation `session-participants` (un joueur répond), recharge le lobby → le MJ et
 *   les joueurs présents voient les réponses apparaître sans rafraîchir ;
 * - à chaque invalidation `session-status` (le MJ démarre la partie), recharge le lobby et, si la
 *   session est passée `ACTIVE`, signale [sessionStarted] pour faire basculer tout le monde vers
 *   l'écran de jeu.
 *
 * [invite] convie un joueur supplémentaire (oubli, ou refus accidentel à réarmer) : l'appel
 * serveur renvoie le lobby à jour, publié aussitôt dans l'état partagé ; les autres écrans du
 * groupe, eux, le reçoivent par le chemin temps réel habituel.
 *
 * [start] déclenche le démarrage réel côté MJ (`POST /sessions/{id}/start`) ; la bascule vers le
 * jeu passe ensuite par le même chemin temps réel que pour les joueurs (rechargement + détection
 * `ACTIVE`), plus un rechargement immédiat en secours si la notification n'était pas reçue.
 *
 * Cycle de vie aligné sur les autres écrans temps réel : abonnement à l'init, désabonnement dans
 * [onCleared].
 *
 * @param sessionId Identifiant de la session dont on suit le lobby.
 * @param activeGroupId Groupe actif (canal temps réel à suivre).
 * @property getSessionLobby Use case de rechargement du lobby.
 * @property inviteToLobby Use case d'invitation d'un joueur dans le lobby ouvert (MJ).
 * @property startSession Use case de démarrage réel de la session (MJ).
 * @property lobbyState État partagé du lobby, mis à jour sur invalidation.
 * @property invalidationBus Bus d'invalidation temps réel.
 * @property subscriptions Registre des abonnements temps réel.
 * @property uiMessageBus Bus de messages UI (toasts d'erreur au démarrage).
 */
class SessionLobbyViewModel(
    private val sessionId: String,
    activeGroupId: StateFlow<String?>,
    private val getSessionLobby: GetSessionLobbyUseCase,
    private val inviteToLobby: InviteToLobbyUseCase,
    private val startSession: StartSessionUseCase,
    private val lobbyState: SessionLobbyState,
    private val invalidationBus: InvalidationBus,
    private val subscriptions: RealtimeSubscriptions,
    private val uiMessageBus: UiMessageBus,
) : ViewModel() {

    private val groupChannel: String? = activeGroupId.value?.let { "group:$it" }

    /** Passe à `true` quand la session est démarrée (`ACTIVE`) : la page navigue vers le jeu. */
    private val _sessionStarted = MutableStateFlow(false)
    val sessionStarted: StateFlow<Boolean> = _sessionStarted.asStateFlow()

    init {
        groupChannel?.let { subscriptions.subscribe(it) }
        viewModelScope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource == "session-participants" ||
                    invalidation.resource == "session-status"
                ) {
                    reload()
                }
            }
        }
    }

    /**
     * Convie un joueur supplémentaire au salon d'attente (MJ) : joueur oublié, ou joueur ayant
     * refusé par erreur — le serveur le repasse alors « en attente ». Sur succès, le lobby
     * renvoyé remplace l'état partagé ; sur échec, un toast d'erreur est émis.
     *
     * @param userId Identifiant du joueur à convier.
     */
    fun invite(userId: String) {
        viewModelScope.launch {
            inviteToLobby(sessionId, listOf(userId)).fold(
                onSuccess = { lobby ->
                    lobbyState.updateLobby(lobby)
                    uiMessageBus.emit(UiMessage.success("Invitation envoyée"))
                },
                onFailure = { err -> uiMessageBus.emit(UiMessage.error(err.message)) },
            )
        }
    }

    /**
     * Démarre réellement la session (MJ). Sur succès, recharge le lobby : la détection du statut
     * `ACTIVE` déclenche la bascule vers l'écran de jeu (comme pour les joueurs, via le temps réel).
     * Sur échec, un toast d'erreur est émis.
     */
    fun start() {
        viewModelScope.launch {
            startSession(sessionId).fold(
                onSuccess = { reload() },
                onFailure = { err -> uiMessageBus.emit(UiMessage.error(err.message)) },
            )
        }
    }

    /** Acquitte la navigation vers le jeu (évite de re-naviguer au retour arrière). */
    fun consumeNavigation() {
        _sessionStarted.value = false
    }

    /**
     * Recharge le lobby depuis le serveur et rafraîchit l'état partagé (best-effort). Si la session
     * est passée `ACTIVE`, arme le signal de bascule vers l'écran de jeu.
     */
    private fun reload() {
        viewModelScope.launch {
            getSessionLobby(sessionId).fold(
                onSuccess = { lobby ->
                    lobbyState.updateLobby(lobby)
                    if (lobby.status == SESSION_STATUS_ACTIVE) _sessionStarted.value = true
                },
                onFailure = { /* best-effort : on conserve l'état courant */ },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupChannel?.let { subscriptions.unsubscribe(it) }
    }
}
