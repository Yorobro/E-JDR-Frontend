package eu.ejdr.presentation.features.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.shared.fold
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel du salon d'attente : rend le lobby **réactif en temps réel**.
 *
 * Le rendu lit toujours l'état partagé [SessionLobbyState] (alimenté à l'ouverture par le MJ ou
 * à l'acceptation par un joueur). Ce ViewModel s'abonne au canal du groupe (`group:{id}`) et, à
 * chaque invalidation `session-participants` (émise par le backend quand un joueur répond),
 * recharge le lobby via [GetSessionLobbyUseCase] et met à jour l'état partagé — de sorte que le
 * MJ **et** les joueurs présents voient les réponses apparaître sans rafraîchir.
 *
 * Cycle de vie aligné sur les autres écrans temps réel (cf. `CampaignDetailViewModel`) :
 * abonnement à l'init, désabonnement dans [onCleared].
 *
 * @param sessionId Identifiant de la session dont on suit le lobby.
 * @param activeGroupId Groupe actif (canal temps réel à suivre).
 * @property getSessionLobby Use case de rechargement du lobby.
 * @property lobbyState État partagé du lobby, mis à jour sur invalidation.
 * @property invalidationBus Bus d'invalidation temps réel.
 * @property subscriptions Registre des abonnements temps réel.
 */
class SessionLobbyViewModel(
    private val sessionId: String,
    activeGroupId: StateFlow<String?>,
    private val getSessionLobby: GetSessionLobbyUseCase,
    private val lobbyState: SessionLobbyState,
    private val invalidationBus: InvalidationBus,
    private val subscriptions: RealtimeSubscriptions,
) : ViewModel() {

    private val groupChannel: String? = activeGroupId.value?.let { "group:$it" }

    init {
        groupChannel?.let { subscriptions.subscribe(it) }
        viewModelScope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource == "session-participants") reload()
            }
        }
    }

    /** Recharge le lobby depuis le serveur et rafraîchit l'état partagé (best-effort). */
    private fun reload() {
        viewModelScope.launch {
            getSessionLobby(sessionId).fold(
                onSuccess = { lobbyState.updateLobby(it) },
                onFailure = { /* best-effort : on conserve l'état courant */ },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupChannel?.let { subscriptions.unsubscribe(it) }
    }
}
