package eu.ejdr.presentation.features.charactersheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la page détail d'une fiche de personnage.
 *
 * Charge la fiche complète ([sheet]) par son identifiant, gère le mode édition ([isEditing])
 * et la sauvegarde. La page tient l'état des champs en cours d'édition et appelle [save] avec
 * la fiche reconstruite.
 *
 * @param sheetId Identifiant de la fiche affichée.
 * @property getById Use case de récupération du détail d'une fiche.
 * @property update Use case de mise à jour d'une fiche.
 * @property getCampaigns Use case de récupération des campagnes rattachées (onglet Campagnes).
 */
class CharacterSheetDetailViewModel(
    private val sheetId: String,
    private val getById: GetCharacterSheetUseCase,
    private val update: UpdateCharacterSheetUseCase,
    private val getCampaigns: GetSheetCampaignsUseCase,
) : ViewModel() {

    private val _sheet = MutableStateFlow<CharacterSheet?>(null)
    val sheet: StateFlow<CharacterSheet?> = _sheet.asStateFlow()

    private val _campaigns = MutableStateFlow<List<SheetCampaign>>(emptyList())
    val campaigns: StateFlow<List<SheetCampaign>> = _campaigns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Recharge la fiche complète (et ses campagnes rattachées) depuis le serveur. */
    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            getById(sheetId).fold(
                onSuccess = { _sheet.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
            getCampaigns(sheetId).fold(
                onSuccess = { _campaigns.value = it },
                onFailure = { /* onglet vide : ne pas écraser l'erreur principale */ },
            )
            _isLoading.value = false
        }
    }

    /** Passe en mode édition. */
    fun startEdit() {
        _error.value = null
        _isEditing.value = true
    }

    /** Annule l'édition (sans sauvegarder). */
    fun cancelEdit() {
        _error.value = null
        _isEditing.value = false
    }

    /** Sauvegarde la fiche éditée ; en cas de succès, sort du mode édition et met à jour l'état. */
    fun save(edited: CharacterSheet) {
        viewModelScope.launch {
            _isLoading.value = true
            update(edited).fold(
                onSuccess = {
                    _sheet.value = it
                    _isEditing.value = false
                    _error.value = null
                },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }
}
