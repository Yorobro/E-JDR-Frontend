package eu.ejdr.presentation.features.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListMySessionInvitationsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.RespondToInvitationUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.shared.feedback.UiMessage
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.session.entities.SessionInvitation
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel des invitations de **session** reçues par le joueur courant.
 *
 * Miroir de [eu.ejdr.presentation.features.friendgroup.InvitationListViewModel] : liste les
 * invitations en attente et écoute l'[InvalidationBus] (`session-invitations`) pour se rafraîchir
 * en temps réel quand un MJ ouvre un lobby.
 *
 * **Acceptation** : le joueur rejoint le lobby en une étape orchestrée ici — on répond au serveur,
 * on **active le groupe** de la campagne (prérequis d'accès aux features), on charge le lobby et
 * ses membres, puis on signale [navigateToLobby] pour que la page navigue vers le salon d'attente
 * en **lecture seule** (`canManage = false`). **Refus** : simple réponse + rechargement.
 *
 * @property listMyInvitations Use case de listing des invitations de session en attente.
 * @property respondToInvitation Use case d'acceptation / refus d'une invitation.
 * @property getSessionLobby Use case de chargement du lobby (statut + participants).
 * @property getGroup Use case de détail d'un groupe (pour résoudre les pseudos des participants).
 * @property activeGroupState État global du groupe actif, activé à l'acceptation.
 * @property lobbyState État partagé du lobby, alimenté avant la navigation.
 * @property invalidationBus Bus d'invalidation temps réel (déclenche un rechargement).
 * @property uiMessageBus Bus de messages UI (toasts de succès / erreur).
 */
class SessionInvitationListViewModel(
    private val listMyInvitations: ListMySessionInvitationsUseCase,
    private val respondToInvitation: RespondToInvitationUseCase,
    private val getSessionLobby: GetSessionLobbyUseCase,
    private val getGroup: GetGroupUseCase,
    private val activeGroupState: ActiveGroupState,
    private val lobbyState: SessionLobbyState,
    private val invalidationBus: InvalidationBus,
    private val uiMessageBus: UiMessageBus,
) : ViewModel() {

    private val _invitations = MutableStateFlow<List<SessionInvitation>>(emptyList())
    val invitations: StateFlow<List<SessionInvitation>> = _invitations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Destination de lobby à ouvrir après acceptation, ou `null`. Consommée par la page. */
    private val _navigateToLobby = MutableStateFlow<LobbyDestination?>(null)
    val navigateToLobby: StateFlow<LobbyDestination?> = _navigateToLobby.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource == "session-invitations") load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            listMyInvitations().fold(
                onSuccess = { list -> _invitations.value = list; _error.value = null },
                onFailure = { err -> _error.value = err.message },
            )
            _isLoading.value = false
        }
    }

    /**
     * Accepte une invitation : réponse serveur, activation du groupe, chargement du lobby, puis
     * signal de navigation vers le salon d'attente (vue joueur).
     */
    fun accept(invitation: SessionInvitation) {
        viewModelScope.launch {
            respondToInvitation(invitation.sessionId, accept = true).fold(
                onSuccess = { joinLobby(invitation) },
                onFailure = { err ->
                    _error.value = err.message
                    uiMessageBus.emit(UiMessage.error(err.message))
                },
            )
        }
    }

    /** Refuse une invitation : réponse serveur puis rechargement de la liste. */
    fun decline(invitation: SessionInvitation) {
        viewModelScope.launch {
            respondToInvitation(invitation.sessionId, accept = false).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Invitation refusée"))
                    load()
                },
                onFailure = { err ->
                    _error.value = err.message
                    uiMessageBus.emit(UiMessage.error(err.message))
                },
            )
        }
    }

    /** Acquitte la navigation vers le lobby (évite de re-naviguer au retour arrière). */
    fun consumeNavigation() {
        _navigateToLobby.value = null
    }

    /**
     * Charge le lobby et ses membres, active le groupe de la campagne, dépose le lobby dans l'état
     * partagé (en **lecture seule**), puis signale la navigation. Sur échec, un message est émis.
     */
    private suspend fun joinLobby(invitation: SessionInvitation) {
        getSessionLobby(invitation.sessionId).fold(
            onSuccess = { lobby ->
                val members = getGroup(invitation.groupId).fold(
                    onSuccess = { detail -> detail.members },
                    onFailure = { emptyList() },
                )
                // Activer le groupe débloque l'accès aux features (campagnes, fiches, lobby…).
                activeGroupState.select(invitation.groupId)
                lobbyState.open(lobby, members, canManage = false)
                _error.value = null
                uiMessageBus.emit(UiMessage.success("Invitation acceptée"))
                _navigateToLobby.value = LobbyDestination(invitation.sessionId, invitation.title)
                load()
            },
            onFailure = { err ->
                _error.value = err.message
                uiMessageBus.emit(UiMessage.error(err.message))
            },
        )
    }

    /** Destination de navigation vers le salon d'attente d'une session. */
    data class LobbyDestination(val sessionId: String, val title: String)
}
