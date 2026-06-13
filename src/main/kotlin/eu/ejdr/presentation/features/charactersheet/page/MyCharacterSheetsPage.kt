package eu.ejdr.presentation.features.charactersheet.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.features.charactersheet.MyCharacterSheetsViewModel
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCard
import eu.ejdr.presentation.features.charactersheet.component.ConfirmDeleteSheetDialog
import eu.ejdr.presentation.features.charactersheet.component.CreateCharacterSheetDialog
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page « Mes fiches » (composant INTELLIGENT).
 *
 * Crée un [MyCharacterSheetsViewModel] retenu par la destination et observe son état.
 * Affiche une carte par fiche, un bouton de création et gère localement les modals.
 *
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun MyCharacterSheetsPage(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel {
        MyCharacterSheetsViewModel(
            get<ListCharacterSheetsUseCase>(),
            get<CreateCharacterSheetUseCase>(),
            get<DeleteCharacterSheetUseCase>(),
        )
    }
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CharacterSheet?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppButton(
            label = "Ajouter une fiche",
            onClick = { showCreate = true },
            leadingIcon = Icons.Filled.Add,
        )

        FormError(message = error)

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

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = AppTheme.dimens.sm),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                ) {
                    items(sheets, key = { it.id }) { sheet ->
                        CharacterSheetCard(
                            sheet = sheet,
                            onDelete = { pendingDelete = sheet },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
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
