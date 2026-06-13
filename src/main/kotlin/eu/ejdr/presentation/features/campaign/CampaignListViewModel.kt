package eu.ejdr.presentation.features.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.campaign.abstraction.usecase.CreateCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.DeleteCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.campaign.entities.Campaign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran liste des campagnes.
 *
 * Charge les campagnes de l'utilisateur courant ([campaigns]) au démarrage, et expose la
 * création et la suppression. Retenu par la destination, son état survit à la recomposition.
 * Toute erreur métier est exposée via [error] (message prêt à afficher) ; aucune exception
 * ne remonte (les use cases renvoient un `Result`).
 *
 * @property listCampaigns Use case de listing.
 * @property createCampaign Use case de création.
 * @property deleteCampaign Use case de suppression.
 */
class CampaignListViewModel(
    private val listCampaigns: ListCampaignsUseCase,
    private val createCampaign: CreateCampaignUseCase,
    private val deleteCampaign: DeleteCampaignUseCase,
) : ViewModel() {

    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Recharge la liste des campagnes depuis le serveur. */
    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            listCampaigns().fold(
                onSuccess = { list ->
                    _campaigns.value = list
                    _error.value = null
                },
                onFailure = { campaignError -> _error.value = campaignError.message },
            )
            _isLoading.value = false
        }
    }

    /**
     * Crée une campagne puis recharge la liste en cas de succès.
     *
     * @param name Nom saisi par l'utilisateur.
     */
    fun create(name: String) {
        viewModelScope.launch {
            createCampaign(name).fold(
                onSuccess = {
                    _error.value = null
                    load()
                },
                onFailure = { campaignError -> _error.value = campaignError.message },
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
                    load()
                },
                onFailure = { campaignError -> _error.value = campaignError.message },
            )
        }
    }
}
