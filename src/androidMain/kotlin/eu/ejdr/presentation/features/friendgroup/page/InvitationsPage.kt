package eu.ejdr.presentation.features.friendgroup.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import eu.ejdr.presentation.shared.component.base.AppSpinner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.friendgroup.abstraction.usecase.AcceptInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeclineInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyInvitationsUseCase
import eu.ejdr.presentation.features.friendgroup.InvitationListViewModel
import eu.ejdr.presentation.features.friendgroup.component.InvitationCard
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/** Invitations reçues (Android) : accepter / refuser une invitation à un groupe. */
@Composable
fun InvitationsPage(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel {
        InvitationListViewModel(
            get<ListMyInvitationsUseCase>(),
            get<AcceptInvitationUseCase>(),
            get<DeclineInvitationUseCase>(),
        )
    }

    val invitations by viewModel.invitations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = "Invitations", style = AppTextStyle.Title)
        FormError(message = error)

        when {
            isLoading && invitations.isEmpty() ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AppSpinner(modifier = Modifier.align(Alignment.Center))
                }

            invitations.isEmpty() ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AppText(
                        text = "Aucune invitation en attente.",
                        style = AppTextStyle.Body,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                ) {
                    items(invitations, key = { it.id }) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            onAccept = { viewModel.accept(invitation.id) },
                            onDecline = { viewModel.decline(invitation.id) },
                        )
                    }
                }
        }
    }
}
