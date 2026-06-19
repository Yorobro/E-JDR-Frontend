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
 * ViewModel **générique** de gestion d'un catalogue d'éléments de référence, paramétré par [type]
 * et **scopé au groupe actif** (D9). Observe [activeGroupId] : recharge les éléments du groupe à
 * chaque changement, vide la liste et lève [needsGroup] quand aucun groupe n'est sélectionné. Une
 * instance par destination (un type à la fois).
 *
 * @param type Catégorie gérée (détermine l'endpoint via son slug).
 * @property activeGroupId Identifiant du groupe actif (null = aucun groupe sélectionné).
 * @property listItems Use case de listing (par type et groupe).
 * @property createItem Use case de création (admin requis côté serveur).
 * @property deleteItem Use case de suppression.
 */
class ReferenceListViewModel(
    private val type: ReferenceType,
    private val activeGroupId: StateFlow<String?>,
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

    /** Vrai quand aucun groupe n'est actif : l'UI invite alors à en choisir/créer un. */
    private val _needsGroup = MutableStateFlow(false)
    val needsGroup: StateFlow<Boolean> = _needsGroup.asStateFlow()

    /**
     * Catalogue des compétences du groupe actif, proposé au dialog de création d'une formation pour
     * la liaison formation→compétences. Vide pour les autres types (non chargé).
     */
    private val _availableCompetences = MutableStateFlow<List<ReferenceItem>>(emptyList())
    val availableCompetences: StateFlow<List<ReferenceItem>> = _availableCompetences.asStateFlow()

    init {
        viewModelScope.launch {
            activeGroupId.collect { groupId -> reload(groupId) }
        }
    }

    /** Recharge la liste du type pour le groupe actif (ou vide + onboarding si aucun groupe). */
    fun load() {
        viewModelScope.launch { reload(activeGroupId.value) }
    }

    private suspend fun reload(groupId: String?) {
        if (groupId == null) {
            _needsGroup.value = true
            _items.value = emptyList()
            _availableCompetences.value = emptyList()
            return
        }
        _needsGroup.value = false
        _isLoading.value = true
        listItems(type, groupId).fold(
            onSuccess = { list -> _items.value = list; _error.value = null },
            onFailure = { error -> _error.value = error.message },
        )
        _isLoading.value = false
        loadAvailableCompetences(groupId)
    }

    /**
     * Charge le catalogue des compétences du groupe (pour le picker du dialog de formation).
     * No-op silencieux pour les autres types ou en cas d'échec (le picker reste vide).
     */
    private suspend fun loadAvailableCompetences(groupId: String) {
        if (type != ReferenceType.FORMATION) return
        listItems(ReferenceType.COMPETENCE, groupId).fold(
            onSuccess = { list -> _availableCompetences.value = list },
            onFailure = { _availableCompetences.value = emptyList() },
        )
    }

    /**
     * Crée un élément dans le groupe actif puis recharge la liste en cas de succès.
     *
     * [stat]/[bonus] s'appliquent aux formations/peuples, [competenceIds] à la seule formation ;
     * pour les types simples, l'appelant passe `null`/`null`/`emptyList`.
     */
    fun create(
        name: String,
        stat: String? = null,
        bonus: Int? = null,
        competenceIds: List<String> = emptyList(),
    ) {
        val groupId = activeGroupId.value ?: return
        viewModelScope.launch {
            createItem(type, name, groupId, stat, bonus, competenceIds).fold(
                onSuccess = { _error.value = null; reload(groupId) },
                onFailure = { error -> _error.value = error.message },
            )
        }
    }

    /** Supprime un élément puis recharge la liste en cas de succès. */
    fun delete(itemId: String) {
        viewModelScope.launch {
            deleteItem(type, itemId).fold(
                onSuccess = { _error.value = null; reload(activeGroupId.value) },
                onFailure = { error -> _error.value = error.message },
            )
        }
    }
}
