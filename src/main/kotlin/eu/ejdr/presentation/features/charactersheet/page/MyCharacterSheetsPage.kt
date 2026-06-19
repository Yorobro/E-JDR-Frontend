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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.features.charactersheet.MyCharacterSheetsViewModel
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCard
import eu.ejdr.presentation.features.charactersheet.component.ConfirmDeleteSheetDialog
import eu.ejdr.presentation.features.charactersheet.component.CreateCharacterSheetDialog
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.shared.component.atomic.AppFab
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

private val MinTileWidth = 180.dp
private val GridBottomPadding = 96.dp

/**
 * Page « Mes fiches » (composant INTELLIGENT).
 *
 * Crée un [MyCharacterSheetsViewModel] retenu par la destination et observe son état. Affiche les
 * fiches en grille de tuiles adaptative, un FAB de création en bas à droite et gère localement
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
        )
    }
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val needsGroup by viewModel.needsGroup.collectAsStateWithLifecycle()

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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.dimens.xl),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                FormError(message = error)
                CharacterSheetGrid(
                    sheets = sheets,
                    isLoading = isLoading,
                    onOpenSheet = onOpenSheet,
                    onDeleteRequest = { pendingDelete = it },
                )
            }

            AppFab(
                onClick = { showCreate = true },
                contentDescription = "Ajouter une fiche",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppTheme.dimens.xl),
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

/**
 * Zone de contenu de la liste des fiches (composant bête).
 *
 * Affiche, selon l'état : un indicateur de chargement initial, un message si vide, ou la grille
 * de tuiles adaptative. Extrait de [MyCharacterSheetsPage] pour garder cette dernière concise.
 *
 * @param sheets Fiches à afficher.
 * @param isLoading Indique si un chargement est en cours.
 * @param onOpenSheet Callback d'ouverture du détail d'une fiche (id + nom).
 * @param onDeleteRequest Callback de demande de suppression d'une fiche.
 */
@Composable
private fun CharacterSheetGrid(
    sheets: List<CharacterSheet>,
    isLoading: Boolean,
    onOpenSheet: (id: String, name: String) -> Unit,
    onDeleteRequest: (CharacterSheet) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && sheets.isEmpty() ->
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppTheme.colors.primary,
                )

            sheets.isEmpty() ->
                AppText(
                    text = "Aucune fiche pour le moment.",
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.align(Alignment.Center),
                )

            else -> LazyVerticalGrid(
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
                    CharacterSheetCard(
                        sheet = sheet,
                        onClick = { onOpenSheet(sheet.id, sheet.name) },
                        onDelete = { onDeleteRequest(sheet) },
                    )
                }
            }
        }
    }
}
