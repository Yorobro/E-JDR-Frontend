package eu.ejdr.presentation.features.charactersheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ExportCharacterSheetPdfUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.LinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListSheetReferencesUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UnlinkSheetReferenceUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.application.shared.getOrElse
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
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
 * @property exportPdf Use case de récupération du PDF (binaire) de la fiche.
 * @property fileSaver Port d'enregistrement du fichier via le dialogue natif « Enregistrer sous ».
 * @property listReferenceItems Use case de listing du catalogue d'un type (dropdowns + dialogs).
 * @property listSheetReferences Use case de listing des éléments N‑N rattachés à la fiche.
 * @property linkSheetReference Use case de rattachement d'un élément N‑N à la fiche.
 * @property unlinkSheetReference Use case de détachement d'un élément N‑N de la fiche.
 */
class CharacterSheetDetailViewModel(
    private val sheetId: String,
    private val activeGroupId: String?,
    private val getById: GetCharacterSheetUseCase,
    private val update: UpdateCharacterSheetUseCase,
    private val getCampaigns: GetSheetCampaignsUseCase,
    private val exportPdf: ExportCharacterSheetPdfUseCase,
    private val fileSaver: FileSaver,
    private val listReferenceItems: ListReferenceItemsUseCase,
    private val listSheetReferences: ListSheetReferencesUseCase,
    private val linkSheetReference: LinkSheetReferenceUseCase,
    private val unlinkSheetReference: UnlinkSheetReferenceUseCase,
) : ViewModel() {

    private val _sheet = MutableStateFlow<CharacterSheet?>(null)
    val sheet: StateFlow<CharacterSheet?> = _sheet.asStateFlow()

    private val _campaigns = MutableStateFlow<List<SheetCampaign>>(emptyList())
    val campaigns: StateFlow<List<SheetCampaign>> = _campaigns.asStateFlow()

    /** Catalogues N‑1 de l'utilisateur (pour les dropdowns formation/peuple). */
    private val _formations = MutableStateFlow<List<ReferenceItem>>(emptyList())
    val formations: StateFlow<List<ReferenceItem>> = _formations.asStateFlow()

    private val _peoples = MutableStateFlow<List<ReferenceItem>>(emptyList())
    val peoples: StateFlow<List<ReferenceItem>> = _peoples.asStateFlow()

    /** Éléments N‑N rattachés à la fiche, par type liable. */
    private val _linked = MutableStateFlow<Map<ReferenceType, List<ReferenceItem>>>(emptyMap())
    val linked: StateFlow<Map<ReferenceType, List<ReferenceItem>>> = _linked.asStateFlow()

    /** Catalogues N‑N de l'utilisateur (pour proposer des éléments à rattacher), par type liable. */
    private val _catalogues = MutableStateFlow<Map<ReferenceType, List<ReferenceItem>>>(emptyMap())
    val catalogues: StateFlow<Map<ReferenceType, List<ReferenceItem>>> = _catalogues.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    init {
        load()
    }

    /** Types de référence rattachables en N‑N à une fiche. */
    private val linkableTypes = ReferenceType.entries.filter { it.linkable }

    /** Recharge la fiche complète, ses campagnes, et les données de référence (catalogues + liaisons). */
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
            loadReferences()
            _isLoading.value = false
        }
    }

    /**
     * Charge les catalogues N‑1 (formations/peuples) pour les dropdowns, et — pour chaque type
     * liable — les éléments rattachés à la fiche + le catalogue de l'utilisateur. Les échecs de
     * référence n'écrasent pas l'erreur principale de la fiche (sections simplement vides).
     */
    private suspend fun loadReferences() {
        // Les éléments rattachés à la fiche (N‑N) dépendent de la fiche, pas du groupe : toujours chargés.
        val linked = mutableMapOf<ReferenceType, List<ReferenceItem>>()
        for (type in linkableTypes) {
            linked[type] = listSheetReferences(sheetId, type).getOrElse { emptyList() }
        }
        _linked.value = linked

        // Les catalogues (formations/peuples + catalogues liables) appartiennent au groupe actif :
        // sans groupe actif, on n'a rien à proposer (sections de sélection vides).
        val groupId = activeGroupId
        if (groupId == null) {
            _formations.value = emptyList()
            _peoples.value = emptyList()
            _catalogues.value = emptyMap()
            return
        }
        _formations.value = listReferenceItems(ReferenceType.FORMATION, groupId).getOrElse { emptyList() }
        _peoples.value = listReferenceItems(ReferenceType.PEUPLE, groupId).getOrElse { emptyList() }

        val catalogues = mutableMapOf<ReferenceType, List<ReferenceItem>>()
        for (type in linkableTypes) {
            catalogues[type] = listReferenceItems(type, groupId).getOrElse { emptyList() }
        }
        _catalogues.value = catalogues
    }

    /** Rattache un élément du catalogue à la fiche (N‑N), puis recharge les liaisons du type. */
    fun linkRef(type: ReferenceType, itemId: String) {
        viewModelScope.launch {
            linkSheetReference(sheetId, type, itemId).fold(
                onSuccess = { _error.value = null; reloadLinked(type) },
                onFailure = { _error.value = it.message },
            )
        }
    }

    /** Détache un élément de la fiche (N‑N), puis recharge les liaisons du type. */
    fun unlinkRef(type: ReferenceType, itemId: String) {
        viewModelScope.launch {
            unlinkSheetReference(sheetId, type, itemId).fold(
                onSuccess = { _error.value = null; reloadLinked(type) },
                onFailure = { _error.value = it.message },
            )
        }
    }

    /** Recharge la liste des éléments rattachés pour un seul type (après link/unlink). */
    private suspend fun reloadLinked(type: ReferenceType) {
        val items = listSheetReferences(sheetId, type).getOrElse { return }
        _linked.value = _linked.value.toMutableMap().apply { put(type, items) }
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

    /**
     * Sauvegarde la fiche éditée ; en cas de succès, sort du mode édition puis **recharge** la fiche.
     *
     * On ne stocke PAS la réponse du PUT directement : elle ne contient pas les blocs résolus
     * `formation`/`peuple` (statistique bonus + compétences dérivées), calculés uniquement côté GET.
     * Recharger garantit que l'écran reflète immédiatement les bonus et compétences de la
     * formation/peuple sélectionnés, sans avoir à quitter puis revenir sur la fiche.
     */
    fun save(edited: CharacterSheet) {
        viewModelScope.launch {
            _isLoading.value = true
            update(edited).fold(
                onSuccess = {
                    _isEditing.value = false
                    _error.value = null
                    refresh()
                },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    /** Recharge la fiche et les données de référence dérivées (formation/peuple/compétences). */
    private suspend fun refresh() {
        getById(sheetId).fold(
            onSuccess = { _sheet.value = it },
            onFailure = { _error.value = it.message },
        )
        loadReferences()
    }

    /**
     * Exporte la fiche courante : récupère le PDF puis ouvre « Enregistrer sous »
     * (nom par défaut « fiche-{nom}.pdf »). Une annulation utilisateur n'est pas une erreur.
     */
    fun export() {
        val current = _sheet.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            exportPdf(sheetId).fold(
                onSuccess = { bytes ->
                    fileSaver.save("fiche-${current.name}.pdf", bytes)
                    _error.value = null
                },
                onFailure = { _error.value = it.message },
            )
            _isExporting.value = false
        }
    }
}
