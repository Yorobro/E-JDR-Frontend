package eu.ejdr.presentation.features.reference.page

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.reference.abstraction.usecase.CreateReferenceItemUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.features.reference.abstraction.usecase.DeleteReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UpdateReferenceItemUseCase
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.reference.ReferenceListViewModel
import eu.ejdr.presentation.features.reference.component.ConfirmDeleteReferenceDialog
import eu.ejdr.presentation.features.reference.component.ReferenceCard
import eu.ejdr.presentation.features.reference.component.ReferenceFormDialog
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.EmptyState
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.molecule.SkeletonGrid
import eu.ejdr.presentation.shared.component.organism.PageHeader
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

private val MinTileWidth = 180.dp
private val GridBottomPadding = 96.dp
private val ReferenceCardHeight = 120.dp

/**
 * Page **générique** de gestion d'un catalogue d'éléments de référence (composant INTELLIGENT).
 *
 * Paramétrée par [type] : crée un [ReferenceListViewModel] retenu par la destination, affiche les
 * éléments en grille, un FAB de création et gère localement les modals. Une seule page pour les six
 * catégories (clone de `CampaignListPage`).
 *
 * @param type Catégorie gérée.
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun ReferenceListPage(
    type: ReferenceType,
    modifier: Modifier = Modifier,
) {
    val activeGroupState = koinInject<ActiveGroupState>()
    val canEdit by activeGroupState.canEdit.collectAsStateWithLifecycle()
    val viewModel = koinViewModel {
        ReferenceListViewModel(
            type,
            activeGroupState.activeGroupId,
            get<ListReferenceItemsUseCase>(),
            get<CreateReferenceItemUseCase>(),
            get<UpdateReferenceItemUseCase>(),
            get<DeleteReferenceItemUseCase>(),
            get<UiMessageBus>(),
        )
    }
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val needsGroup by viewModel.needsGroup.collectAsStateWithLifecycle()
    val availableCompetences by viewModel.availableCompetences.collectAsStateWithLifecycle()
    val competenceNames by viewModel.competenceNames.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var pendingEdit by remember { mutableStateOf<ReferenceItem?>(null) }
    var pendingDelete by remember { mutableStateOf<ReferenceItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (needsGroup) {
            AppText(
                text = "Choisis ou crée un groupe pour gérer ses ${type.label.lowercase()}.",
                style = AppTextStyle.Body,
                color = AppTheme.colors.muted,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(AppTheme.dimens.xl),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                PageHeader(
                    title = type.label.replaceFirstChar { it.uppercase() },
                    subtitle = "${items.size} ${if (items.size > 1) "éléments" else "élément"}",
                    action = if (canEdit) {
                        {
                            AppButton(
                                label = "Ajouter",
                                onClick = { showCreate = true },
                                leadingIcon = Icons.Default.Add,
                            )
                        }
                    } else null,
                )
                FormError(message = error)
                ReferenceGrid(
                    type = type,
                    items = items,
                    isLoading = isLoading,
                    competenceNames = competenceNames,
                    canEdit = canEdit,
                    onEditRequest = { pendingEdit = it },
                    onDeleteRequest = { pendingDelete = it },
                    onCreateRequest = { showCreate = true },
                )
            }
        }
    }

    ReferenceFormDialogs(
        type = type,
        showCreate = showCreate,
        pendingEdit = pendingEdit,
        availableCompetences = availableCompetences,
        onCreateDismiss = { showCreate = false },
        onCreateConfirm = { name, stat, bonus, competenceIds, protectionPoints, description ->
            showCreate = false
            viewModel.create(name, stat, bonus, competenceIds, protectionPoints, description)
        },
        onEditDismiss = { pendingEdit = null },
        onEditConfirm = { id, name, stat, bonus, competenceIds, protectionPoints, description ->
            pendingEdit = null
            viewModel.update(id, name, stat, bonus, competenceIds, protectionPoints, description)
        },
    )

    pendingDelete?.let { item ->
        ConfirmDeleteReferenceDialog(
            itemName = item.name,
            onConfirm = { pendingDelete = null; viewModel.delete(item.id) },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Modals de création et d'édition (mêmes champs, libellés et pré-remplissage distincts). */
@Composable
private fun ReferenceFormDialogs(
    type: ReferenceType,
    showCreate: Boolean,
    pendingEdit: ReferenceItem?,
    availableCompetences: List<ReferenceItem>,
    onCreateDismiss: () -> Unit,
    onCreateConfirm: (String, String?, Int?, List<String>, Int?, String?) -> Unit,
    onEditDismiss: () -> Unit,
    onEditConfirm: (String, String, String?, Int?, List<String>, Int?, String?) -> Unit,
) {
    if (showCreate) {
        ReferenceFormDialog(
            type = type,
            title = "Nouvel élément : ${type.singularLabel}",
            label = "Nom (${type.singularLabel})",
            confirmLabel = "Créer",
            onDismiss = onCreateDismiss,
            onConfirm = onCreateConfirm,
            availableCompetences = availableCompetences,
        )
    }

    pendingEdit?.let { item ->
        ReferenceFormDialog(
            type = type,
            title = "Modifier : ${type.singularLabel}",
            label = "Nom (${type.singularLabel})",
            confirmLabel = "Enregistrer",
            initial = item,
            onDismiss = onEditDismiss,
            onConfirm = { name, stat, bonus, competenceIds, protectionPoints, description ->
                onEditConfirm(item.id, name, stat, bonus, competenceIds, protectionPoints, description)
            },
            availableCompetences = availableCompetences,
        )
    }
}

/** Zone de contenu : skeleton de chargement initial, état vide accueillant, ou grille de tuiles. */
@Composable
private fun ReferenceGrid(
    type: ReferenceType,
    items: List<ReferenceItem>,
    isLoading: Boolean,
    competenceNames: Map<String, String>,
    canEdit: Boolean,
    onEditRequest: (ReferenceItem) -> Unit,
    onDeleteRequest: (ReferenceItem) -> Unit,
    onCreateRequest: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && items.isEmpty() ->
                SkeletonGrid(itemHeight = ReferenceCardHeight)

            items.isEmpty() ->
                EmptyState(
                    icon = Icons.Default.Category,
                    title = "Aucun élément",
                    message = "Ajoute ton premier élément de référence.",
                    actionLabel = if (canEdit) "Ajouter" else null,
                    onAction = if (canEdit) onCreateRequest else null,
                    modifier = Modifier.align(Alignment.Center),
                )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = MinTileWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = AppTheme.dimens.sm, bottom = GridBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                items(items, key = { it.id }) { item ->
                    ReferenceCard(
                        item = item,
                        type = type,
                        competenceNames = competenceNames,
                        onEdit = if (canEdit) ({ onEditRequest(item) }) else null,
                        onDelete = if (canEdit) ({ onDeleteRequest(item) }) else null,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
