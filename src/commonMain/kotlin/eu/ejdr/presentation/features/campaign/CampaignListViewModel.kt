package eu.ejdr.presentation.features.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.campaign.abstraction.usecase.CreateCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.DeleteCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.presentation.shared.feedback.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran liste des campagnes, **scopé au groupe actif** (D9).
 *
 * Observe [activeGroupId] : recharge les campagnes du groupe à chaque changement de groupe actif.
 * Quand aucun groupe n'est sélectionné, vide la liste et lève [needsGroup] (écran d'onboarding
 * « choisis un groupe »). La création rattache la campagne au groupe actif. Toute erreur métier
 * est exposée via [error] ; aucune exception ne remonte (les use cases renvoient un `Result`).
 *
 * @property activeGroupId Identifiant du groupe actif (null = aucun groupe sélectionné).
 * @property listCampaigns Use case de listing (par groupe).
 * @property createCampaign Use case de création (dans un groupe).
 * @property deleteCampaign Use case de suppression.
 */
class CampaignListViewModel(
    private val activeGroupId: StateFlow<String?>,
    private val listCampaigns: ListCampaignsUseCase,
    private val createCampaign: CreateCampaignUseCase,
    private val deleteCampaign: DeleteCampaignUseCase,
    private val uiMessageBus: UiMessageBus,
) : ViewModel() {

    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Vrai quand aucun groupe n'est actif : l'UI invite alors à en choisir/créer un. */
    private val _needsGroup = MutableStateFlow(false)
    val needsGroup: StateFlow<Boolean> = _needsGroup.asStateFlow()

    init {
        viewModelScope.launch {
            activeGroupId.collect { groupId -> reload(groupId) }
        }
    }

    /** Recharge la liste des campagnes du groupe actif (ou vide + onboarding si aucun groupe). */
    fun load() {
        viewModelScope.launch { reload(activeGroupId.value) }
    }

    private suspend fun reload(groupId: String?) {
        if (groupId == null) {
            _needsGroup.value = true
            _campaigns.value = emptyList()
            return
        }
        _needsGroup.value = false
        _isLoading.value = true
        listCampaigns(groupId).fold(
            onSuccess = { list ->
                _campaigns.value = list
                _error.value = null
            },
            onFailure = { campaignError -> _error.value = campaignError.message },
        )
        _isLoading.value = false
    }

    /**
     * Crée une campagne dans le groupe actif puis recharge la liste en cas de succès.
     *
     * @param name Nom saisi par l'utilisateur.
     */
    fun create(name: String) {
        val groupId = activeGroupId.value ?: return
        viewModelScope.launch {
            createCampaign(name, groupId).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Campagne créée"))
                    reload(groupId)
                },
                onFailure = { campaignError ->
                    _error.value = campaignError.message
                    uiMessageBus.emit(UiMessage.error(campaignError.message))
                },
            )
        }
    }

    /**
     * Supprime une campagne puis recharge la liste en cas de succès.
     *
     * @param id Identifiant de la campagne à supprimer.
     */
    fun delete(id: String) {
        viewModelScope.launch {
            deleteCampaign(id).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Campagne supprimée"))
                    reload(activeGroupId.value)
                },
                onFailure = { campaignError ->
                    _error.value = campaignError.message
                    uiMessageBus.emit(UiMessage.error(campaignError.message))
                },
            )
        }
    }
}
