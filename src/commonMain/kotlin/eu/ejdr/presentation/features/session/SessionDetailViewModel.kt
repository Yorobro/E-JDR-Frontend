package eu.ejdr.presentation.features.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.application.shared.feedback.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la page détail d'une session.
 *
 * Charge la session par son identifiant ([session]), gère la sauvegarde (titre + date) et la
 * suppression. Après une suppression réussie, [deleted] passe à `true` pour que la page
 * revienne en arrière.
 *
 * Gère aussi l'**ouverture du lobby** (réservé au MJ) : charge les joueurs sélectionnables du
 * groupe actif (tous les membres **sauf le MJ courant**, qui accède à la session via son rôle),
 * puis convie les joueurs cochés via [openLobby].
 *
 * Autorisation : les actions de gestion (édition, suppression, ouverture du lobby) sont réservées
 * au **MJ de la campagne** parente ([isGameMaster] : `gameMasterId == utilisateur courant`), comme
 * pour le détail de campagne. Le rôle dans le groupe d'amis ne confère aucun droit sur une
 * campagne dont on n'est pas le MJ.
 *
 * @param sessionId Identifiant de la session affichée.
 * @param activeGroupId Identifiant du groupe actif (pour retrouver la campagne parente et son MJ).
 * @property getById Use case de récupération du détail d'une session.
 * @property update Use case de mise à jour d'une session.
 * @property deleteSession Use case de suppression d'une session.
 * @property createLobby Use case d'ouverture du lobby (convie les joueurs).
 * @property getGroup Use case de récupération du détail d'un groupe (pour lister ses membres).
 * @property getCurrentUser Use case d'identification du MJ courant (exclu de la liste à cocher).
 * @property listCampaigns Use case de listing des campagnes (pour résoudre le MJ de la campagne).
 * @property lobbyState État partagé où déposer le lobby ouvert, à destination de l'écran de lobby.
 */
class SessionDetailViewModel(
    private val sessionId: String,
    private val activeGroupId: StateFlow<String?>,
    private val getById: GetSessionUseCase,
    private val update: UpdateSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val createLobby: CreateLobbyUseCase,
    private val getGroup: GetGroupUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val listCampaigns: ListCampaignsUseCase,
    private val uiMessageBus: UiMessageBus,
    private val lobbyState: SessionLobbyState,
) : ViewModel() {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** Vrai si l'utilisateur courant est le MJ de la campagne parente (seul à pouvoir gérer la session). */
    private val _isGameMaster = MutableStateFlow(false)
    val isGameMaster: StateFlow<Boolean> = _isGameMaster.asStateFlow()

    /** Joueurs sélectionnables pour le lobby : membres du groupe actif **hors MJ courant**. */
    private val _selectableMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val selectableMembers: StateFlow<List<GroupMember>> = _selectableMembers.asStateFlow()

    private val _membersLoading = MutableStateFlow(false)
    val membersLoading: StateFlow<Boolean> = _membersLoading.asStateFlow()

    /** Passe à `true` une fois le lobby ouvert, pour piloter la suite (rechargement / navigation). */
    private val _lobbyOpened = MutableStateFlow(false)
    val lobbyOpened: StateFlow<Boolean> = _lobbyOpened.asStateFlow()

    init {
        load()
    }

    /** Recharge la session depuis le serveur, puis résout si l'utilisateur en est le MJ. */
    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            getById(sessionId).fold(
                onSuccess = { s ->
                    _session.value = s
                    _error.value = null
                    resolveGameMaster(s.campaignId)
                },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    /**
     * Détermine si l'utilisateur courant est le MJ de la campagne parente (compare `gameMasterId`
     * au compte courant via la liste des campagnes du groupe actif). Échec silencieux ⇒ `false`.
     *
     * @param campaignId Identifiant de la campagne parente de la session.
     */
    private fun resolveGameMaster(campaignId: String) {
        viewModelScope.launch {
            val groupId = activeGroupId.value ?: return@launch
            val currentUserId = getCurrentUser().fold(onSuccess = { it.id }, onFailure = { null })
                ?: return@launch
            listCampaigns(groupId).fold(
                onSuccess = { campaigns ->
                    _isGameMaster.value = campaigns.firstOrNull { it.id == campaignId }?.gameMasterId == currentUserId
                },
                onFailure = { _isGameMaster.value = false },
            )
        }
    }

    /** Sauvegarde le titre et la date édités ; met à jour l'état en cas de succès. */
    fun save(title: String, date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            update(sessionId, title, date).fold(
                onSuccess = {
                    _session.value = it
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Session enregistrée"))
                },
                onFailure = {
                    _error.value = it.message
                    uiMessageBus.emit(UiMessage.error(it.message))
                },
            )
            _isLoading.value = false
        }
    }

    /** Supprime la session ; en cas de succès, signale [deleted] pour revenir en arrière. */
    fun delete() {
        viewModelScope.launch {
            _isLoading.value = true
            deleteSession(sessionId).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Session supprimée"))
                    _deleted.value = true
                },
                onFailure = {
                    _error.value = it.message
                    uiMessageBus.emit(UiMessage.error(it.message))
                },
            )
            _isLoading.value = false
        }
    }

    /**
     * Charge la liste des joueurs sélectionnables pour le lobby : les membres du groupe [groupId],
     * **sans le MJ courant** (il rejoint la session via son rôle, sans s'inviter).
     *
     * @param groupId Identifiant du groupe actif (celui de la campagne parente).
     */
    fun loadSelectableMembers(groupId: String) {
        viewModelScope.launch {
            _membersLoading.value = true
            val currentUserId = getCurrentUser().fold(
                onSuccess = { it.id },
                onFailure = { null },
            )
            getGroup(groupId).fold(
                onSuccess = { detail ->
                    _selectableMembers.value = detail.members.filter { it.userId != currentUserId }
                    _error.value = null
                },
                onFailure = { _error.value = it.message },
            )
            _membersLoading.value = false
        }
    }

    /**
     * Ouvre le lobby en conviant les joueurs cochés. En cas de succès, recharge la session
     * (le statut passe `LOBBY`) et signale [lobbyOpened].
     *
     * @param participantUserIds Identifiants des joueurs cochés (hors MJ).
     */
    fun openLobby(participantUserIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            createLobby(sessionId, participantUserIds).fold(
                onSuccess = { lobby ->
                    _error.value = null
                    // Dépose le lobby (+ membres conviables) dans l'état partagé, à destination de
                    // l'écran de lobby vers lequel la page va naviguer.
                    lobbyState.open(lobby, _selectableMembers.value)
                    uiMessageBus.emit(UiMessage.success("Lobby ouvert"))
                    _lobbyOpened.value = true
                    load()
                },
                onFailure = {
                    _error.value = it.message
                    uiMessageBus.emit(UiMessage.error(it.message))
                },
            )
            _isLoading.value = false
        }
    }

    /**
     * Acquitte la navigation vers l'écran de lobby : remet [lobbyOpened] à `false` pour que le
     * retour arrière sur le détail ne re-déclenche pas la navigation.
     */
    fun consumeLobbyOpened() {
        _lobbyOpened.value = false
    }
}
