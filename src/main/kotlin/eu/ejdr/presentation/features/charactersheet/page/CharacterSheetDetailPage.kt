package eu.ejdr.presentation.features.charactersheet.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ExportCharacterSheetPdfUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.presentation.features.charactersheet.CharacterSheetDetailViewModel
import eu.ejdr.presentation.features.charactersheet.component.CampagnesTab
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetFormState
import eu.ejdr.presentation.features.charactersheet.component.CombatTab
import eu.ejdr.presentation.features.charactersheet.component.IdentiteTab
import eu.ejdr.presentation.features.charactersheet.component.InventaireTab
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppTabs
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

private val TabTitles = listOf("Identité", "Combat", "Inventaire", "Campagnes")

/**
 * Page détail d'une fiche de personnage (composant INTELLIGENT).
 *
 * Charge la fiche via [CharacterSheetDetailViewModel] et l'affiche en QUATRE onglets
 * (Identité / Combat / Inventaire / Campagnes). L'en-tête (titre + barre d'action) et la barre
 * d'onglets restent FIXES ; seul le contenu de l'onglet défile. Édition et état de formulaire GLOBAUX :
 * éditer dans un onglet puis changer d'onglet conserve les modifications ; Enregistrer persiste
 * toute la fiche.
 *
 * @param id Identifiant de la fiche (sert au chargement).
 * @param name Nom de la fiche (affiché en titre, évite d'attendre le chargement).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun CharacterSheetDetailPage(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        CharacterSheetDetailViewModel(
            sheetId = id,
            getById = get<GetCharacterSheetUseCase>(),
            update = get<UpdateCharacterSheetUseCase>(),
            getCampaigns = get<GetSheetCampaignsUseCase>(),
            exportPdf = get<ExportCharacterSheetPdfUseCase>(),
            fileSaver = get<FileSaver>(),
        )
    }
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val campaigns by viewModel.campaigns.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.lg),
    ) {
        AppText(text = name, style = AppTextStyle.Title)
        FormError(message = error)

        sheet?.let { loaded ->
            CharacterSheetDetailContent(
                sheet = loaded,
                campaigns = campaigns,
                isEditing = isEditing,
                isSaving = isLoading,
                isExporting = isExporting,
                onStartEdit = viewModel::startEdit,
                onCancelEdit = viewModel::cancelEdit,
                onSave = viewModel::save,
                onExport = viewModel::export,
            )
        }
    }
}

/**
 * Contenu de la fiche : en-tête fixe (barre d'action + onglets) puis contenu d'onglet défilant.
 *
 * Le [CharacterSheetFormState] (réamorcé à chaque nouvelle [sheet]) et l'onglet sélectionné sont
 * tenus ICI, au-dessus du switch d'onglet, pour que les éditions persistent d'un onglet à l'autre.
 */
@Composable
private fun CharacterSheetDetailContent(
    sheet: CharacterSheet,
    campaigns: List<SheetCampaign>,
    isEditing: Boolean,
    isSaving: Boolean,
    isExporting: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (CharacterSheet) -> Unit,
    onExport: () -> Unit,
) {
    val form = remember(sheet) { CharacterSheetFormState(sheet) }
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.lg),
    ) {
        DetailActionBar(
            isEditing = isEditing,
            isSaving = isSaving,
            isExporting = isExporting,
            canSave = form.isNameValid,
            onStartEdit = onStartEdit,
            onCancelEdit = onCancelEdit,
            onSave = { onSave(form.toCharacterSheet()) },
            onExport = onExport,
        )
        AppTabs(tabs = TabTitles, selectedIndex = selectedTab, onSelect = { selectedTab = it })

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            TabContent(
                selectedTab = selectedTab,
                sheet = sheet,
                campaigns = campaigns,
                isEditing = isEditing,
                form = form,
            )
        }
    }
}

/** Barre d'action globale : Modifier/Annuler + Enregistrer (visible en édition). */
@Composable
private fun DetailActionBar(
    isEditing: Boolean,
    isSaving: Boolean,
    isExporting: Boolean,
    canSave: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm)) {
        if (isEditing) {
            AppButton(label = "Annuler", onClick = onCancelEdit, variant = ButtonVariant.Secondary)
            AppButton(
                label = "Enregistrer",
                onClick = onSave,
                enabled = canSave,
                loading = isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AppButton(label = "Modifier", onClick = onStartEdit, variant = ButtonVariant.Secondary)
            AppButton(
                label = "Exporter",
                onClick = onExport,
                variant = ButtonVariant.Secondary,
                loading = isExporting,
            )
        }
    }
}

/** Aiguille vers le contenu de l'onglet sélectionné (form partagé → édition persistante). */
@Composable
private fun TabContent(
    selectedTab: Int,
    sheet: CharacterSheet,
    campaigns: List<SheetCampaign>,
    isEditing: Boolean,
    form: CharacterSheetFormState,
) {
    when (selectedTab) {
        0 -> IdentiteTab(sheet, isEditing, form)
        1 -> CombatTab(sheet, isEditing, form)
        2 -> InventaireTab(sheet, isEditing, form)
        else -> CampagnesTab(campaigns)
    }
}
