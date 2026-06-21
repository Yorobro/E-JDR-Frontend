package eu.ejdr.presentation.features.charactersheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran « Fiches du groupe », **scopé au groupe actif** (D9/D10).
 *
 * Visibilité « tout le groupe » : observe [activeGroupId] et recharge les fiches du groupe à chaque
 * changement de groupe actif. Quand aucun groupe n'est sélectionné, vide la liste et lève
 * [needsGroup] (onboarding « choisis un groupe »). La création rattache la fiche au groupe actif.
 * Aucune exception ne remonte (les use cases renvoient un `Result`).
 *
 * @property activeGroupId Identifiant du groupe actif (null = aucun groupe sélectionné).
 * @property listSheets Use case de listing (par groupe).
 * @property createSheet Use case de création (dans un groupe).
 * @property deleteSheet Use case de suppression.
 * @property getCurrentUser Use case exposant l'utilisateur courant (pour distinguer ses fiches).
 * @property invalidationBus Bus temps réel : recharge la liste à chaque invalidation « character-sheets ».
 */
class MyCharacterSheetsViewModel(
    private val activeGroupId: StateFlow<String?>,
    private val listSheets: ListCharacterSheetsUseCase,
    private val createSheet: CreateCharacterSheetUseCase,
    private val deleteSheet: DeleteCharacterSheetUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val invalidationBus: InvalidationBus,
) : ViewModel() {

    private val _sheets = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val sheets: StateFlow<List<CharacterSheet>> = _sheets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Vrai quand aucun groupe n'est actif : l'UI invite alors à en choisir/créer un. */
    private val _needsGroup = MutableStateFlow(false)
    val needsGroup: StateFlow<Boolean> = _needsGroup.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            activeGroupId.collect { groupId -> reload(groupId) }
        }
        viewModelScope.launch {
            when (val r = getCurrentUser()) {
                is Result.Success -> _currentUserId.value = r.value.id
                is Result.Failure -> Unit
            }
        }
        // Temps réel : recharge la liste dès qu'une invalidation « character-sheets » arrive
        // (ex. une fiche créée/supprimée depuis un autre appareil du même utilisateur).
        viewModelScope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource == "character-sheets") {
                    reload(activeGroupId.value)
                }
            }
        }
    }

    /** Recharge la liste des fiches du groupe actif (ou vide + onboarding si aucun groupe). */
    fun load() {
        viewModelScope.launch { reload(activeGroupId.value) }
    }

    private suspend fun reload(groupId: String?) {
        if (groupId == null) {
            _needsGroup.value = true
            _sheets.value = emptyList()
            return
        }
        _needsGroup.value = false
        _isLoading.value = true
        listSheets(groupId).fold(
            onSuccess = { list ->
                _sheets.value = list
                _error.value = null
            },
            onFailure = { error -> _error.value = error.message },
        )
        _isLoading.value = false
    }

    /** Crée une fiche dans le groupe actif puis recharge la liste en cas de succès. */
    fun create(name: String) {
        val groupId = activeGroupId.value ?: return
        viewModelScope.launch {
            createSheet(name, groupId).fold(
                onSuccess = {
                    _error.value = null
                    reload(groupId)
                },
                onFailure = { error -> _error.value = error.message },
            )
        }
    }

    /** Supprime une fiche puis recharge la liste en cas de succès. */
    fun delete(id: String) {
        viewModelScope.launch {
            deleteSheet(id).fold(
                onSuccess = {
                    _error.value = null
                    reload(activeGroupId.value)
                },
                onFailure = { error -> _error.value = error.message },
            )
        }
    }
}
