package eu.ejdr.presentation.features.charactersheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran « Mes fiches ».
 *
 * Charge les fiches de l'utilisateur courant et expose la création et la suppression. Retenu
 * par la destination, son état survit à la recomposition. Aucune exception ne remonte.
 *
 * @property listSheets Use case de listing.
 * @property createSheet Use case de création.
 * @property deleteSheet Use case de suppression.
 */
class MyCharacterSheetsViewModel(
    private val listSheets: ListCharacterSheetsUseCase,
    private val createSheet: CreateCharacterSheetUseCase,
    private val deleteSheet: DeleteCharacterSheetUseCase,
) : ViewModel() {

    private val _sheets = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val sheets: StateFlow<List<CharacterSheet>> = _sheets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Recharge la liste des fiches. */
    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            listSheets().fold(
                onSuccess = { list ->
                    _sheets.value = list
                    _error.value = null
                },
                onFailure = { error -> _error.value = error.message },
            )
            _isLoading.value = false
        }
    }

    /** Crée une fiche puis recharge la liste en cas de succès. */
    fun create(name: String) {
        viewModelScope.launch {
            createSheet(name).fold(
                onSuccess = {
                    _error.value = null
                    load()
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
                    load()
                },
                onFailure = { error -> _error.value = error.message },
            )
        }
    }
}
