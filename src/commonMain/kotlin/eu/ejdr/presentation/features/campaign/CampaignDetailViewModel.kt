package eu.ejdr.presentation.features.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.AcceptCharacterUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListPendingCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.RefuseCharacterUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.application.shared.feedback.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la page détail d'une campagne.
 *
 * Charge les fiches ACCEPTÉES rattachées à la campagne ([characters], lecture seule) et les demandes
 * de rattachement en attente ([pendingCharacters], visibles du MJ). Expose la validation et le refus
 * d'une demande, ainsi que les [sessions] de la campagne et leur création.
 *
 * Autorisation : seul le **MJ de la campagne** (son créateur, `gameMasterId == utilisateur courant`)
 * peut gérer ses sessions et statuer sur les demandes ([isGameMaster]). Le rôle dans le groupe d'amis
 * ne confère aucun droit sur une campagne dont on n'est pas le MJ ; les autres (joueurs comme admins)
 * voient seulement les personnages acceptés et les sessions en lecture.
 *
 * @param campaignId Identifiant de la campagne affichée.
 * @param activeGroupId Identifiant du groupe actif (pour retrouver la campagne et son MJ).
 * @property listCampaignCharacters Use case de listing des fiches acceptées.
 * @property listPendingCharacters Use case de listing des demandes en attente.
 * @property acceptCharacter Use case de validation d'une demande.
 * @property refuseCharacter Use case de refus d'une demande.
 * @property listCampaignSessions Use case de listing des sessions de la campagne.
 * @property createSession Use case de création d'une session.
 * @property listCampaigns Use case de listing des campagnes (pour résoudre le MJ de la campagne).
 * @property getCurrentUser Use case exposant l'utilisateur courant (pour comparer au MJ).
 */
class CampaignDetailViewModel(
    private val campaignId: String,
    private val activeGroupId: StateFlow<String?>,
    private val listCampaignCharacters: ListCampaignCharactersUseCase,
    private val listPendingCharacters: ListPendingCharactersUseCase,
    private val acceptCharacter: AcceptCharacterUseCase,
    private val refuseCharacter: RefuseCharacterUseCase,
    private val listCampaignSessions: ListCampaignSessionsUseCase,
    private val createSession: CreateSessionUseCase,
    private val listCampaigns: ListCampaignsUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val uiMessageBus: UiMessageBus,
) : ViewModel() {

    private val _characters = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val characters: StateFlow<List<CharacterSheet>> = _characters.asStateFlow()

    private val _pendingCharacters = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val pendingCharacters: StateFlow<List<CharacterSheet>> = _pendingCharacters.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    /** Vrai si l'utilisateur courant est le MJ de cette campagne (seul à pouvoir la gérer). */
    private val _isGameMaster = MutableStateFlow(false)
    val isGameMaster: StateFlow<Boolean> = _isGameMaster.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
        resolveGameMaster()
    }

    /** Recharge les fiches acceptées, les demandes en attente et les sessions. */
    fun load() {
        viewModelScope.launch {
            listCampaignCharacters(campaignId).fold(
                onSuccess = { _characters.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
            loadPending()
            loadSessions()
        }
    }

    /**
     * Détermine si l'utilisateur courant est le MJ de cette campagne (compare `gameMasterId` au
     * compte courant via la liste des campagnes du groupe). Échec silencieux ⇒ `isGameMaster=false`.
     */
    private fun resolveGameMaster() {
        viewModelScope.launch {
            val groupId = activeGroupId.value ?: return@launch
            val currentUserId =
                when (val r = getCurrentUser()) {
                    is Result.Success -> r.value.id
                    is Result.Failure -> return@launch
                }
            listCampaigns(groupId).fold(
                onSuccess = { campaigns ->
                    val campaign = campaigns.firstOrNull { it.id == campaignId }
                    _isGameMaster.value = campaign?.gameMasterId == currentUserId
                },
                onFailure = { _isGameMaster.value = false },
            )
        }
    }

    /** Recharge les demandes en attente (réservées au MJ ; échec silencieux pour un joueur). */
    private fun loadPending() {
        viewModelScope.launch {
            listPendingCharacters(campaignId).fold(
                onSuccess = { _pendingCharacters.value = it },
                onFailure = { _pendingCharacters.value = emptyList() },
            )
        }
    }

    /** Recharge uniquement les sessions de la campagne. */
    fun loadSessions() {
        viewModelScope.launch {
            listCampaignSessions(campaignId).fold(
                onSuccess = { _sessions.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
        }
    }

    /** Crée une session puis recharge la liste des sessions. */
    fun createSession(title: String, date: String) {
        viewModelScope.launch {
            createSession(campaignId, title, date).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Session créée"))
                    loadSessions()
                },
                onFailure = {
                    _error.value = it.message
                    uiMessageBus.emit(UiMessage.error(it.message))
                },
            )
        }
    }

    /** Valide (MJ) une demande de rattachement en attente, puis recharge. */
    fun accept(characterSheetId: String) {
        viewModelScope.launch {
            acceptCharacter(campaignId, characterSheetId).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Demande acceptée"))
                    load()
                },
                onFailure = {
                    _error.value = it.message
                    uiMessageBus.emit(UiMessage.error(it.message))
                },
            )
        }
    }

    /** Refuse (MJ) une demande de rattachement en attente (fiche supprimée), puis recharge. */
    fun refuse(characterSheetId: String) {
        viewModelScope.launch {
            refuseCharacter(campaignId, characterSheetId).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Demande refusée (fiche supprimée)"))
                    load()
                },
                onFailure = {
                    _error.value = it.message
                    uiMessageBus.emit(UiMessage.error(it.message))
                },
            )
        }
    }
}
