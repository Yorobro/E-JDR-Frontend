package eu.ejdr.presentation.features.friendgroup.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.friendgroup.abstraction.usecase.CreateGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeleteGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyGroupsUseCase
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.friendgroup.GroupListViewModel
import eu.ejdr.presentation.features.friendgroup.component.CreateGroupDialog
import eu.ejdr.presentation.features.friendgroup.component.GroupCard
import eu.ejdr.presentation.shared.component.atomic.AppFab
import eu.ejdr.presentation.shared.component.molecule.EmptyState
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.molecule.SkeletonList
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

private val ListBottomPadding = 96.dp
private val GroupCardHeight = 110.dp

@Composable
fun GroupListPage(
    onNavigateToDetail: (FriendGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        GroupListViewModel(
            get<ListMyGroupsUseCase>(),
            get<CreateGroupUseCase>(),
            get<DeleteGroupUseCase>(),
        )
    }
    val activeGroupState = koinInject<ActiveGroupState>()

    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val activeGroupId by activeGroupState.activeGroupId.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppTheme.dimens.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        ) {
            FormError(message = error)

            when {
                isLoading && groups.isEmpty() ->
                    SkeletonList(itemHeight = GroupCardHeight)

                groups.isEmpty() ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        EmptyState(
                            icon = Icons.Default.Group,
                            title = "Aucun groupe",
                            message = "Crée un groupe pour jouer avec tes amis.",
                            actionLabel = "Créer un groupe",
                            onAction = { showCreate = true },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ListBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                    ) {
                        items(groups, key = { it.id }) { group ->
                            GroupCard(
                                group = group,
                                isActive = group.id == activeGroupId,
                                onActivate = { activeGroupState.select(group.id) },
                                onClick = { onNavigateToDetail(group) },
                                onDelete = if (group.myRole == "ADMIN") {
                                    { viewModel.delete(group.id) }
                                } else null,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
            }
        }

        AppFab(
            onClick = { showCreate = true },
            contentDescription = "Créer un groupe",
            modifier = Modifier.align(Alignment.BottomEnd).padding(AppTheme.dimens.xl),
        )
    }

    if (showCreate) {
        CreateGroupDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.create(name)
            },
        )
    }
}
