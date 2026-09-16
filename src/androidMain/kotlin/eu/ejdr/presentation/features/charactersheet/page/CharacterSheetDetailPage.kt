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
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ExportCharacterSheetPdfUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.reference.abstraction.usecase.LinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListSheetReferencesUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UnlinkSheetReferenceUseCase
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.features.charactersheet.CharacterSheetDetailViewModel
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetFormState
import eu.ejdr.presentation.features.charactersheet.component.CombatTab
import eu.ejdr.presentation.features.charactersheet.component.IdentiteTab
import eu.ejdr.presentation.features.charactersheet.component.InventaireTab
import eu.ejdr.presentation.features.charactersheet.component.SheetReferences
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppTabs
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.molecule.RemoteChangeBanner
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.AppTreatment
import eu.ejdr.presentation.shared.theme.ProvideTreatment
import org.koin.compose.koinInject

private val TabTitles = listOf("Identité", "Combat", "Inventaire")

/**
 * Page détail d'une fiche de personnage (Android).
 *
 * 3 onglets (Identité / Combat / Inventaire) ; en-tête (« Nom - NomCampagne ») + barre d'onglets
 * fixes, contenu d'onglet défilant. Édition globale persistante entre onglets. Toute la logique de
 * rendu (onglets, stats, références, formulaire) vient des composants communs. La campagne de la
 * fiche (« 1 fiche = 1 campagne ») est affichée dans le titre, plus dans un onglet.
 */
@Composable
fun CharacterSheetDetailPage(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    val activeGroupState = koinInject<ActiveGroupState>()
    val viewModel = koinViewModel {
        CharacterSheetDetailViewModel(
            sheetId = id,
            activeGroupId = activeGroupState.activeGroupId,
            getById = get<GetCharacterSheetUseCase>(),
            update = get<UpdateCharacterSheetUseCase>(),
            exportPdf = get<ExportCharacterSheetPdfUseCase>(),
            fileSaver = get<FileSaver>(),
            listReferenceItems = get<ListReferenceItemsUseCase>(),
            listSheetReferences = get<ListSheetReferencesUseCase>(),
            linkSheetReference = get<LinkSheetReferenceUseCase>(),
            unlinkSheetReference = get<UnlinkSheetReferenceUseCase>(),
            getCurrentUser = get<GetCurrentUserUseCase>(),
            invalidationBus = get<InvalidationBus>(),
            subscriptions = get<RealtimeSubscriptions>(),
        )
    }
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
    val sheetChangedRemotely by viewModel.sheetChangedRemotely.collectAsStateWithLifecycle()
    val canEdit by activeGroupState.canEdit.collectAsStateWithLifecycle()
    val formations by viewModel.formations.collectAsStateWithLifecycle()
    val peoples by viewModel.peoples.collectAsStateWithLifecycle()
    val linked by viewModel.linked.collectAsStateWithLifecycle()
    val catalogues by viewModel.catalogues.collectAsStateWithLifecycle()

    val refs = SheetReferences(
        formations = formations,
        peoples = peoples,
        linked = linked,
        catalogues = catalogues,
        onLink = viewModel::linkRef,
        onUnlink = viewModel::unlinkRef,
    )

    // Écran vitrine : traitement « grimoire assumé » (reliefs/bordures dorés côté AppSurface/AppButton).
    ProvideTreatment(AppTreatment.Rich) {
        Column(
            modifier = modifier.fillMaxSize().padding(AppTheme.dimens.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.lg),
        ) {
            AppText(text = sheetTitle(name, sheet), style = AppTextStyle.Title)
            FormError(message = error)
            if (sheetChangedRemotely) {
                RemoteChangeBanner(
                    onReload = viewModel::reloadFromRemote,
                    onDismiss = viewModel::dismissRemoteChange,
                )
            }

            sheet?.let { loaded ->
                CharacterSheetDetailContent(
                    sheet = loaded,
                    refs = refs,
                    isEditing = isEditing,
                    isSaving = isLoading,
                    canModify = canEdit || isOwner,
                    onStartEdit = viewModel::startEdit,
                    onCancelEdit = viewModel::cancelEdit,
                    onSave = viewModel::save,
                )
            }
        }
    }
}

/**
 * Titre de la fiche : « Nom - NomCampagne » quand la fiche est chargée et **acceptée** dans une
 * campagne, sinon le nom seul (avant chargement, ou tant que le rattachement est PENDING).
 */
private fun sheetTitle(fallbackName: String, sheet: CharacterSheet?): String {
    val name = sheet?.name ?: fallbackName
    val campaign = sheet?.campaignName
    return if (sheet?.linkStatus == "ACCEPTED" && !campaign.isNullOrBlank()) {
        "$name - $campaign"
    } else {
        name
    }
}

@Composable
private fun CharacterSheetDetailContent(
    sheet: CharacterSheet,
    refs: SheetReferences,
    isEditing: Boolean,
    isSaving: Boolean,
    canModify: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (CharacterSheet) -> Unit,
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
            canSave = form.isNameValid,
            canModify = canModify,
            onStartEdit = onStartEdit,
            onCancelEdit = onCancelEdit,
            onSave = { onSave(form.toCharacterSheet()) },
        )
        AppTabs(tabs = TabTitles, selectedIndex = selectedTab, onSelect = { selectedTab = it })

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            TabContent(
                selectedTab = selectedTab,
                sheet = sheet,
                refs = refs,
                isEditing = isEditing,
                form = form,
            )
        }
    }
}

@Composable
private fun DetailActionBar(
    isEditing: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    canModify: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm)) {
        if (canModify && isEditing) {
            AppButton(label = "Annuler", onClick = onCancelEdit, variant = ButtonVariant.Secondary, fillWidth = true)
            AppButton(
                label = "Enregistrer",
                onClick = onSave,
                enabled = canSave,
                loading = isSaving,
                fillWidth = true,
            )
        }
        // Pas d'export PDF sur mobile : réservé au desktop (« Enregistrer sous »).
        if (!isEditing && canModify) {
            AppButton(label = "Modifier", onClick = onStartEdit, variant = ButtonVariant.Secondary, fillWidth = true)
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: Int,
    sheet: CharacterSheet,
    refs: SheetReferences,
    isEditing: Boolean,
    form: CharacterSheetFormState,
) {
    when (selectedTab) {
        0 -> IdentiteTab(sheet, isEditing, form, refs)
        1 -> CombatTab(sheet, isEditing, form, refs)
        else -> InventaireTab(sheet, isEditing, form, refs)
    }
}
