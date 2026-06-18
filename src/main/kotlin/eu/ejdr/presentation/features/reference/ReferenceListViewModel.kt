package eu.ejdr.presentation.features.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.reference.abstraction.usecase.CreateReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.DeleteReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel **générique** de gestion d'un catalogue d'éléments de référence, paramétré par
 * [type]. Charge les éléments de l'utilisateur pour ce type, expose création et suppression.
 * Une instance par destination (un type à la fois) ; clone de `CampaignListViewModel`.
 *
 * @param type Catégorie gérée (détermine l'endpoint via son slug).
 * @property listItems Use case de listing.
 * @property createItem Use case de création.
 * @property deleteItem Use case de suppression.
 */
class ReferenceListViewModel(
    private val type: ReferenceType,
    private val listItems: ListReferenceItemsUseCase,
    private val createItem: CreateReferenceItemUseCase,
    private val deleteItem: DeleteReferenceItemUseCase,
) : ViewModel() {

    private val _items = MutableStateFlow<List<ReferenceItem>>(emptyList())
    val items: StateFlow<List<ReferenceItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Recharge la liste des éléments du type depuis le serveur. */
    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            listItems(type).fold(
                onSuccess = { list -> _items.value = list; _error.value = null },
                onFailure = { error -> _error.value = error.message },
            )
            _isLoading.value = false
        }
    }

    /** Crée un élément puis recharge la liste en cas de succès. */
    fun create(name: String) {
        viewModelScope.launch {
            createItem(type, name).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { error -> _error.value = error.message },
            )
        }
    }

    /** Supprime un élément puis recharge la liste en cas de succès. */
    fun delete(itemId: String) {
        viewModelScope.launch {
            deleteItem(type, itemId).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { error -> _error.value = error.message },
            )
        }
    }
}
