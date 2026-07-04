package eu.ejdr.presentation.features.charactersheet.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import eu.ejdr.presentation.shared.icons.AppIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CopyCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.features.charactersheet.MyCharacterSheetsViewModel
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCard
import eu.ejdr.presentation.features.charactersheet.component.ConfirmDeleteSheetDialog
import eu.ejdr.presentation.features.charactersheet.component.CopyCharacterSheetDialog
import eu.ejdr.presentation.features.charactersheet.component.CreateCharacterSheetDialog
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.modifier.appItemAppearSpec
import eu.ejdr.presentation.shared.component.molecule.EmptyState
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.molecule.SkeletonGrid
import eu.ejdr.presentation.shared.component.organism.PageHeader
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

private val MinTileWidth = 180.dp
private val GridBottomPadding = 96.dp
private val CardHeight = 140.dp

/**
 * Page « Mes fiches » (composant INTELLIGENT).
 *
 * Crée un [MyCharacterSheetsViewModel] retenu par la destination et observe son état. Affiche les
 * fiches en grille de tuiles adaptative, une action de création dans l'en-tête et gère localement
 * l'état d'ouverture des deux modals (création, confirmation de suppression). Le rendu est délégué
 * à des composants bêtes. Le clic sur une tuile ouvre le détail de la fiche.
 *
 * @param onOpenSheet Callback d'ouverture du détail d'une fiche (id + nom).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun MyCharacterSheetsPage(
    onOpenSheet: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeGroupState = koinInject<ActiveGroupState>()
    val viewModel = koinViewModel {
        MyCharacterSheetsViewModel(
            activeGroupState.activeGroupId,
            get<ListCharacterSheetsUseCase>(),
            get<CreateCharacterSheetUseCase>(),
            get<DeleteCharacterSheetUseCase>(),
            get<CopyCharacterSheetUseCase>(),
            get<ListCampaignsUseCase>(),
            get<GetCurrentUserUseCase>(),
            get<InvalidationBus>(),
            get<UiMessageBus>(),
        )
    }
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    val eligibleCampaigns by viewModel.eligibleCampaigns.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val needsGroup by viewModel.needsGroup.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val canEdit by activeGroupState.canEdit.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CharacterSheet?>(null) }
    var pendingCopy by remember { mutableStateOf<CharacterSheet?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (needsGroup) {
            AppText(
                text = "Choisis ou crée un groupe pour voir ses fiches.",
                style = AppTextStyle.Body,
                color = AppTheme.colors.muted,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.dimens.xl),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                PageHeader(
                    title = "Mes fiches",
                    subtitle = "${sheets.size} ${if (sheets.size > 1) "fiches" else "fiche"}",
                    action = {
                        AppButton(
                            label = "Nouvelle fiche",
                            onClick = { showCreate = true },
                            leadingIcon = AppIcons.Add,
                        )
                    },
                )
                FormError(message = error)
                CharacterSheetGrid(
                    sheets = sheets,
                    isLoading = isLoading,
                    canEdit = canEdit,
                    currentUserId = currentUserId,
                    onOpenSheet = onOpenSheet,
                    onDeleteRequest = { pendingDelete = it },
                    onCopyRequest = { pendingCopy = it },
                    onCreateRequest = { showCreate = true },
                )
            }
        }
    }

    SheetDialogs(
        showCreate = showCreate,
        eligibleCampaigns = eligibleCampaigns,
        pendingCopy = pendingCopy,
        pendingDelete = pendingDelete,
        onCreate = { name, campaignId -> showCreate = false; viewModel.create(name, campaignId) },
        onDismissCreate = { showCreate = false },
        onCopy = { sheet, campaignId -> pendingCopy = null; viewModel.copy(sheet.id, campaignId) },
        onDismissCopy = { pendingCopy = null },
        onDelete = { sheet -> pendingDelete = null; viewModel.delete(sheet.id) },
        onDismissDelete = { pendingDelete = null },
    )
}

/**
 * Regroupe les trois modals de la page « Mes fiches » (création, copie, confirmation de
 * suppression). Extrait de [MyCharacterSheetsPage] pour garder cette dernière concise.
 */
@Composable
private fun SheetDialogs(
    showCreate: Boolean,
    eligibleCampaigns: List<Campaign>,
    pendingCopy: CharacterSheet?,
    pendingDelete: CharacterSheet?,
    onCreate: (name: String, campaignId: String) -> Unit,
    onDismissCreate: () -> Unit,
    onCopy: (sheet: CharacterSheet, campaignId: String) -> Unit,
    onDismissCopy: () -> Unit,
    onDelete: (CharacterSheet) -> Unit,
    onDismissDelete: () -> Unit,
) {
    if (showCreate) {
        CreateCharacterSheetDialog(
            campaigns = eligibleCampaigns,
            onDismiss = onDismissCreate,
            onConfirm = onCreate,
        )
    }

    pendingCopy?.let { sheet ->
        CopyCharacterSheetDialog(
            sheetName = sheet.name,
            campaigns = eligibleCampaigns,
            onDismiss = onDismissCopy,
            onConfirm = { campaignId -> onCopy(sheet, campaignId) },
            excludeCampaignId = sheet.campaignId,
        )
    }

    pendingDelete?.let { sheet ->
        ConfirmDeleteSheetDialog(
            sheetName = sheet.name,
            onConfirm = { onDelete(sheet) },
            onDismiss = onDismissDelete,
        )
    }
}

/**
 * Zone de contenu de la liste des fiches (composant bête).
 *
 * Affiche, selon l'état : un skeleton de chargement initial, un état vide accueillant, ou la grille
 * de tuiles adaptative. Extrait de [MyCharacterSheetsPage] pour garder cette dernière concise.
 *
 * @param sheets Fiches à afficher.
 * @param isLoading Indique si un chargement est en cours.
 * @param onOpenSheet Callback d'ouverture du détail d'une fiche (id + nom).
 * @param onDeleteRequest Callback de demande de suppression d'une fiche.
 * @param onCopyRequest Callback de demande de copie d'une fiche vers une autre campagne.
 * @param onCreateRequest Callback d'ouverture du dialog de création.
 */
@Composable
private fun CharacterSheetGrid(
    sheets: List<CharacterSheet>,
    isLoading: Boolean,
    canEdit: Boolean,
    currentUserId: String?,
    onOpenSheet: (id: String, name: String) -> Unit,
    onDeleteRequest: (CharacterSheet) -> Unit,
    onCopyRequest: (CharacterSheet) -> Unit,
    onCreateRequest: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && sheets.isEmpty() ->
                SkeletonGrid(itemHeight = CardHeight)

            sheets.isEmpty() ->
                EmptyState(
                    icon = AppIcons.Person,
                    title = "Aucune fiche pour l'instant",
                    message = "Crée ton premier personnage pour ce groupe.",
                    actionLabel = "Créer une fiche",
                    onAction = onCreateRequest,
                    modifier = Modifier.align(Alignment.Center),
                )

            else -> {
                val appearSpec = appItemAppearSpec()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = MinTileWidth),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = AppTheme.dimens.sm,
                        bottom = GridBottomPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                ) {
                    items(sheets, key = { it.id }) { sheet ->
                        val isOwner = sheet.ownerId == currentUserId
                        val canDelete = canEdit || isOwner
                        CharacterSheetCard(
                            sheet = sheet,
                            onClick = { onOpenSheet(sheet.id, sheet.name) },
                            onCopy = if (isOwner) ({ onCopyRequest(sheet) }) else null,
                            onDelete = if (canDelete) ({ onDeleteRequest(sheet) }) else null,
                            modifier = Modifier.animateItem(fadeInSpec = appearSpec),
                        )
                    }
                }
            }
        }
    }
}
