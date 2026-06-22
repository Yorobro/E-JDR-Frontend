package eu.ejdr.presentation.features.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.presentation.shared.feedback.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la page détail d'une campagne.
 *
 * Charge les fiches rattachées à la campagne ([characters]) et les fiches rattachables
 * ([linkableSheets], filtrées côté back, pour proposer un rattachement). Expose le rattachement
 * et le détachement, ainsi que les [sessions] de la campagne et leur création.
 *
 * @param campaignId Identifiant de la campagne affichée.
 * @property listCampaignCharacters Use case de listing des fiches rattachées.
 * @property listLinkable Use case de listing des fiches rattachables à la campagne.
 * @property linkCharacter Use case de rattachement.
 * @property unlinkCharacter Use case de détachement.
 * @property listCampaignSessions Use case de listing des sessions de la campagne.
 * @property createSession Use case de création d'une session.
 */
class CampaignDetailViewModel(
    private val campaignId: String,
    private val listCampaignCharacters: ListCampaignCharactersUseCase,
    private val listLinkable: ListLinkableCharactersUseCase,
    private val linkCharacter: LinkCharacterToCampaignUseCase,
    private val unlinkCharacter: UnlinkCharacterFromCampaignUseCase,
    private val listCampaignSessions: ListCampaignSessionsUseCase,
    private val createSession: CreateSessionUseCase,
    private val uiMessageBus: UiMessageBus,
) : ViewModel() {

    private val _characters = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val characters: StateFlow<List<CharacterSheet>> = _characters.asStateFlow()

    private val _linkableSheets = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val linkableSheets: StateFlow<List<CharacterSheet>> = _linkableSheets.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Recharge les fiches rattachées, les fiches rattachables et les sessions. */
    fun load() {
        viewModelScope.launch {
            listCampaignCharacters(campaignId).fold(
                onSuccess = { _characters.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
            listLinkable(campaignId).fold(
                onSuccess = { _linkableSheets.value = it },
                onFailure = { _error.value = it.message },
            )
            loadSessions()
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

    /** Rattache la fiche d'un autre joueur à la campagne (MJ), puis recharge. */
    fun link(characterSheetId: String) {
        viewModelScope.launch {
            linkCharacter(campaignId, characterSheetId).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Fiche rattachée"))
                    load()
                },
                onFailure = {
                    _error.value = it.message
                    uiMessageBus.emit(UiMessage.error(it.message))
                },
            )
        }
    }

    /** Détache une fiche de la campagne, puis recharge. */
    fun unlink(characterSheetId: String) {
        viewModelScope.launch {
            unlinkCharacter(campaignId, characterSheetId).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Fiche retirée"))
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
