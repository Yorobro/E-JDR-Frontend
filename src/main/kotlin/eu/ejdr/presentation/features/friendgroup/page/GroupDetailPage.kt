package eu.ejdr.presentation.features.friendgroup.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ChangeMemberRoleUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.InviteMemberUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.RemoveMemberUseCase
import eu.ejdr.presentation.features.friendgroup.GroupDetailViewModel
import eu.ejdr.presentation.features.friendgroup.component.InviteMemberDialog
import eu.ejdr.presentation.features.friendgroup.component.MemberCard
import eu.ejdr.presentation.shared.component.atomic.AppFab
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

private val ListBottomPadding = 96.dp

@Composable
fun GroupDetailPage(
    groupId: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        GroupDetailViewModel(
            groupId,
            get<GetGroupUseCase>(),
            get<InviteMemberUseCase>(),
            get<RemoveMemberUseCase>(),
            get<ChangeMemberRoleUseCase>(),
        )
    }

    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val inviteSuccess by viewModel.inviteSuccess.collectAsStateWithLifecycle()

    var showInvite by remember { mutableStateOf(false) }

    LaunchedEffect(inviteSuccess) {
        if (inviteSuccess) viewModel.clearInviteSuccess()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppTheme.dimens.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        ) {
            FormError(message = error)

            when {
                isLoading && detail == null ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppTheme.colors.primary,
                        )
                    }

                detail != null -> {
                    val d = detail!!
                    val isAdmin = d.myRole == "ADMIN"
                    val adminCount = d.members.count { it.role == "ADMIN" }
                    AppText(
                        text = if (isAdmin) "Mon rôle : Administrateur" else "Mon rôle : Membre",
                        style = AppTextStyle.Body,
                        color = AppTheme.colors.textSecondary,
                    )
                    AppText(text = "Membres (${d.members.size})", style = AppTextStyle.Subtitle)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ListBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
                    ) {
                        items(d.members, key = { it.userId }) { member ->
                            // On masque la rétrogradation du dernier admin (le back la refuse de toute
                            // façon : CannotRemoveLastAdmin). Réservé aux admins.
                            val wouldDemoteLastAdmin = member.role == "ADMIN" && adminCount <= 1
                            MemberCard(
                                member = member,
                                canRemove = d.members.size > 1,
                                canManageRole = isAdmin && !wouldDemoteLastAdmin,
                                onRemove = { viewModel.removeMember(member.userId) },
                                onChangeRole = { newRole -> viewModel.changeRole(member.userId, newRole) },
                            )
                        }
                    }
                }
            }
        }

        AppFab(
            onClick = { showInvite = true },
            contentDescription = "Inviter un membre",
            modifier = Modifier.align(Alignment.BottomEnd).padding(AppTheme.dimens.xl),
        )
    }

    if (showInvite) {
        InviteMemberDialog(
            onDismiss = { showInvite = false },
            onConfirm = { email ->
                showInvite = false
                viewModel.invite(email)
            },
        )
    }
}
