package eu.ejdr.presentation.features.friendgroup.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import eu.ejdr.presentation.shared.component.base.AppSpinner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.friendgroup.abstraction.usecase.AcceptInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeclineInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyInvitationsUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListMySessionInvitationsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.RespondToInvitationUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.domain.features.session.entities.SessionInvitation
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.friendgroup.InvitationListViewModel
import eu.ejdr.presentation.features.friendgroup.component.InvitationCard
import eu.ejdr.presentation.features.session.SessionInvitationListViewModel
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppCard
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Boîte de réception unifiée des invitations (Android) : invitations à une **session** (en tête,
 * car elles mènent à une action immédiate — rejoindre un lobby) puis invitations à un **groupe**.
 *
 * Miroir de la page desktop du même nom. Le titre « Invitations » est fourni par l'[AppTopBar] du
 * NavEntry, la page n'en réaffiche donc pas.
 *
 * @param onOpenLobby Navigation vers le salon d'attente, déclenchée après acceptation d'une
 *   invitation de session (le groupe est alors activé et le lobby chargé).
 */
@Composable
fun InvitationsPage(
    onOpenLobby: (sessionId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupVm = koinViewModel {
        InvitationListViewModel(
            get<ListMyInvitationsUseCase>(),
            get<AcceptInvitationUseCase>(),
            get<DeclineInvitationUseCase>(),
            get<InvalidationBus>(),
        )
    }
    val sessionVm = koinViewModel {
        SessionInvitationListViewModel(
            get<ListMySessionInvitationsUseCase>(),
            get<RespondToInvitationUseCase>(),
            get<GetSessionLobbyUseCase>(),
            get<GetGroupUseCase>(),
            get<ActiveGroupState>(),
            get<SessionLobbyState>(),
            get<InvalidationBus>(),
            get<UiMessageBus>(),
        )
    }

    val groupInvitations by groupVm.invitations.collectAsStateWithLifecycle()
    val groupError by groupVm.error.collectAsStateWithLifecycle()
    val groupLoading by groupVm.isLoading.collectAsStateWithLifecycle()

    val sessionInvitations by sessionVm.invitations.collectAsStateWithLifecycle()
    val sessionError by sessionVm.error.collectAsStateWithLifecycle()
    val sessionLoading by sessionVm.isLoading.collectAsStateWithLifecycle()
    val navigateToLobby by sessionVm.navigateToLobby.collectAsStateWithLifecycle()

    // Après acceptation : le groupe est activé et le lobby chargé, on navigue puis on acquitte.
    LaunchedEffect(navigateToLobby) {
        navigateToLobby?.let { destination ->
            onOpenLobby(destination.sessionId, destination.title)
            sessionVm.consumeNavigation()
        }
    }

    val isEmpty = sessionInvitations.isEmpty() && groupInvitations.isEmpty()
    val isLoading = sessionLoading || groupLoading

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        FormError(message = sessionError ?: groupError)

        when {
            isLoading && isEmpty ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AppSpinner(modifier = Modifier.align(Alignment.Center))
                }

            isEmpty ->
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
                    if (sessionInvitations.isNotEmpty()) {
                        item(key = "session-header") {
                            AppText(text = "Invitations à une session", style = AppTextStyle.Subtitle)
                        }
                        items(sessionInvitations, key = { "session-${it.sessionId}" }) { invitation ->
                            SessionInvitationCard(
                                invitation = invitation,
                                onAccept = { sessionVm.accept(invitation) },
                                onDecline = { sessionVm.decline(invitation) },
                            )
                        }
                    }
                    if (groupInvitations.isNotEmpty()) {
                        item(key = "group-header") {
                            AppText(text = "Invitations à un groupe", style = AppTextStyle.Subtitle)
                        }
                        items(groupInvitations, key = { "group-${it.id}" }) { invitation ->
                            InvitationCard(
                                invitation = invitation,
                                onAccept = { groupVm.accept(invitation.id) },
                                onDecline = { groupVm.decline(invitation.id) },
                            )
                        }
                    }
                }
        }
    }
}

/** Carte d'une invitation de session : titre + campagne/date, avec Accepter / Refuser. */
@Composable
private fun SessionInvitationCard(
    invitation: SessionInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AppCard(onClick = null) {
        Column {
            AppText(text = invitation.title, style = AppTextStyle.Subtitle)
            AppText(
                text = "Campagne ${invitation.campaignName} · ${invitation.date}",
                style = AppTextStyle.Body,
                color = AppTheme.colors.textSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AppTheme.dimens.sm),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
            ) {
                AppButton(
                    label = "Accepter",
                    onClick = onAccept,
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    label = "Refuser",
                    onClick = onDecline,
                    variant = ButtonVariant.Ghost,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
