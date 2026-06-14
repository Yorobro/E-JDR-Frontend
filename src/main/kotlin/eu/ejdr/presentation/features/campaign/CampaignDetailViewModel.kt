package eu.ejdr.presentation.features.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la page détail d'une campagne.
 *
 * Charge les fiches rattachées à la campagne ([characters]) et les fiches rattachables
 * ([linkableSheets], filtrées côté back, pour proposer un rattachement). Expose le rattachement
 * et le détachement.
 *
 * @param campaignId Identifiant de la campagne affichée.
 * @property listCampaignCharacters Use case de listing des fiches rattachées.
 * @property listLinkable Use case de listing des fiches rattachables à la campagne.
 * @property linkCharacter Use case de rattachement.
 * @property unlinkCharacter Use case de détachement.
 */
class CampaignDetailViewModel(
    private val campaignId: String,
    private val listCampaignCharacters: ListCampaignCharactersUseCase,
    private val listLinkable: ListLinkableCharactersUseCase,
    private val linkCharacter: LinkCharacterToCampaignUseCase,
    private val unlinkCharacter: UnlinkCharacterFromCampaignUseCase,
) : ViewModel() {

    private val _characters = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val characters: StateFlow<List<CharacterSheet>> = _characters.asStateFlow()

    private val _linkableSheets = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val linkableSheets: StateFlow<List<CharacterSheet>> = _linkableSheets.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Recharge les fiches rattachées et les fiches rattachables. */
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
        }
    }

    /** Rattache la fiche d'un autre joueur à la campagne (MJ), puis recharge. */
    fun link(characterSheetId: String) {
        viewModelScope.launch {
            linkCharacter(campaignId, characterSheetId).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { _error.value = it.message },
            )
        }
    }

    /** Détache une fiche de la campagne, puis recharge. */
    fun unlink(characterSheetId: String) {
        viewModelScope.launch {
            unlinkCharacter(campaignId, characterSheetId).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { _error.value = it.message },
            )
        }
    }
}
