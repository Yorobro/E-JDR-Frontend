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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.features.charactersheet.MyCharacterSheetsViewModel
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCard
import eu.ejdr.presentation.features.charactersheet.component.ConfirmDeleteSheetDialog
import eu.ejdr.presentation.features.charactersheet.component.CreateCharacterSheetDialog
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.shared.component.atomic.AppFab
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.EmptyState
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.molecule.SkeletonGrid
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

private val MinTileWidth = 160.dp
private val GridBottomPadding = 96.dp
private val CardHeight = 140.dp

/**
 * Page « Mes fiches » Android : grille adaptative de tuiles (1-2 colonnes mobile), FAB de
 * création, dialogs communs. Scopée au groupe actif (onboarding si aucun groupe).
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
            get<GetCurrentUserUseCase>(),
            get<InvalidationBus>(),
        )
    }
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val needsGroup by viewModel.needsGroup.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val canEdit by activeGroupState.canEdit.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CharacterSheet?>(null) }

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
                modifier = Modifier.fillMaxSize().padding(AppTheme.dimens.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                AppText(text = "Mes fiches", style = AppTextStyle.Title)
                FormError(message = error)
                CharacterSheetGrid(
                    sheets = sheets,
                    isLoading = isLoading,
                    canEdit = canEdit,
                    currentUserId = currentUserId,
                    onOpenSheet = onOpenSheet,
                    onDeleteRequest = { pendingDelete = it },
                    onCreateRequest = { showCreate = true },
                )
            }

            AppFab(
                onClick = { showCreate = true },
                contentDescription = "Ajouter une fiche",
                modifier = Modifier.align(Alignment.BottomEnd).padding(AppTheme.dimens.xl),
            )
        }
    }

    if (showCreate) {
        CreateCharacterSheetDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.create(name)
            },
        )
    }

    pendingDelete?.let { sheet ->
        ConfirmDeleteSheetDialog(
            sheetName = sheet.name,
            onConfirm = {
                pendingDelete = null
                viewModel.delete(sheet.id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun CharacterSheetGrid(
    sheets: List<CharacterSheet>,
    isLoading: Boolean,
    canEdit: Boolean,
    currentUserId: String?,
    onOpenSheet: (id: String, name: String) -> Unit,
    onDeleteRequest: (CharacterSheet) -> Unit,
    onCreateRequest: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && sheets.isEmpty() ->
                SkeletonGrid(itemHeight = CardHeight)

            sheets.isEmpty() ->
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "Aucune fiche pour l'instant",
                    message = "Crée ton premier personnage pour ce groupe.",
                    actionLabel = "Créer une fiche",
                    onAction = onCreateRequest,
                    modifier = Modifier.align(Alignment.Center),
                )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = MinTileWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = AppTheme.dimens.sm, bottom = GridBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                items(sheets, key = { it.id }) { sheet ->
                    val canDelete = canEdit || sheet.ownerId == currentUserId
                    CharacterSheetCard(
                        sheet = sheet,
                        onClick = { onOpenSheet(sheet.id, sheet.name) },
                        onDelete = if (canDelete) ({ onDeleteRequest(sheet) }) else null,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
