package eu.ejdr.presentation.features.session.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.InviteToLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.StartSessionUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.features.session.SessionLobbyViewModel
import eu.ejdr.presentation.shared.component.atomic.AppBadge
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.BadgeTone
import eu.ejdr.presentation.shared.component.molecule.EmptyState
import eu.ejdr.presentation.shared.component.organism.AppCard
import eu.ejdr.presentation.shared.component.organism.PageHeader
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Salon d'attente (lobby) d'une session — écran INTELLIGENT, partagé MJ et joueurs.
 *
 * Observe le [SessionLobbyState] partagé (alimenté par le détail de session au `launch` côté MJ,
 * ou par l'acceptation d'une invitation côté joueur) : liste les joueurs conviés et l'état de
 * leur invitation. Le MJ (canManage) peut en convier d'autres et démarrer la session ; un joueur
 * convié voit le même salon en lecture seule. Le MJ peut aussi **reconvier** un joueur ayant
 * refusé : celui-ci reste proposé dans le sélecteur et repasse « En attente » côté serveur.
 *
 * Les réponses des joueurs arrivent **en temps réel** : [SessionLobbyViewModel] s'abonne au canal
 * du groupe et recharge le lobby à chaque invalidation `session-participants`, mettant à jour
 * l'état partagé — l'écran se recompose alors sans action de l'utilisateur. Si l'état est vide
 * (ex. lobby non ouvert, reprise à froid), un état vide invite à repasser par le détail.
 *
 * @param sessionId Identifiant de la session (pour le rechargement temps réel du lobby).
 * @param title Titre de la session (affiché en en-tête).
 * @param onStarted Callback déclenché quand le MJ démarre la session (câblage de l'écran de jeu à venir).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun SessionLobbyPage(
    sessionId: String,
    title: String,
    onStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lobbyState = koinInject<SessionLobbyState>()
    val activeGroupState = koinInject<ActiveGroupState>()
    // Rend le lobby réactif : abonnement au groupe + rechargement sur `session-participants` /
    // `session-status`, et pilote le démarrage réel de la session (MJ).
    val viewModel = koinViewModel {
        SessionLobbyViewModel(
            sessionId = sessionId,
            activeGroupId = activeGroupState.activeGroupId,
            getSessionLobby = get<GetSessionLobbyUseCase>(),
            inviteToLobby = get<InviteToLobbyUseCase>(),
            startSession = get<StartSessionUseCase>(),
            lobbyState = get<SessionLobbyState>(),
            invalidationBus = get<InvalidationBus>(),
            subscriptions = get<RealtimeSubscriptions>(),
            uiMessageBus = get<UiMessageBus>(),
        )
    }
    val lobby by lobbyState.lobby.collectAsStateWithLifecycle()
    val members by lobbyState.members.collectAsStateWithLifecycle()
    // MJ : commandes visibles (inviter, démarrer). Joueur convié : vue en lecture seule.
    val canManage by lobbyState.canManage.collectAsStateWithLifecycle()

    // Quand la session démarre (ACTIVE) — que ce soit par action du MJ ou par bascule temps réel
    // côté joueur —, on navigue vers l'écran de jeu puis on acquitte.
    val sessionStarted by viewModel.sessionStarted.collectAsStateWithLifecycle()
    LaunchedEffect(sessionStarted) {
        if (sessionStarted) {
            onStarted()
            viewModel.consumeNavigation()
        }
    }

    val currentLobby = lobby
    if (currentLobby == null) {
        EmptyState(
            icon = AppIcons.Groups,
            title = "Aucun lobby ouvert",
            message = "Revenez au détail de la session pour lancer le salon d'attente.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    // Joueurs déjà dans le salon (en attente ou présents) et joueurs encore conviables : un
    // joueur ayant refusé reste proposé, pour le cas du refus accidentel.
    val inLobbyIds = currentLobby.participants
        .filterNot { it.status == LOBBY_STATUS_REFUSED }
        .map { it.userId }
        .toSet()
    val invitable = members.filter { it.userId !in inLobbyIds }
    val pseudoOf: (String) -> String = { id -> members.firstOrNull { it.userId == id }?.pseudo?.ifBlank { id } ?: id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        PageHeader(title = title, subtitle = "Salon d'attente")

        AppText(text = "Joueurs conviés", style = AppTextStyle.Subtitle)
        if (currentLobby.participants.isEmpty()) {
            AppText(
                text = "Aucun joueur convié pour le moment.",
                style = AppTextStyle.Body,
                color = AppTheme.colors.textSecondary,
            )
        } else {
            currentLobby.participants.forEach { participant ->
                LobbyParticipantRow(pseudo = pseudoOf(participant.userId), participant = participant)
            }
        }

        // Commandes réservées au MJ : convier d'autres joueurs et démarrer la session.
        // Un joueur convié voit le même salon d'attente, sans ces actions.
        if (canManage) {
            InviteMoreSection(
                invitable = invitable,
                onInvite = { userId -> viewModel.invite(userId) },
            )

            AppButton(
                label = "Commencer la session",
                onClick = { viewModel.start() },
                leadingIcon = AppIcons.Play,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Ligne d'un participant du lobby : pseudo à gauche, pastille d'état d'invitation à droite. */
@Composable
private fun LobbyParticipantRow(
    pseudo: String,
    participant: LobbyParticipant,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(text = pseudo, style = AppTextStyle.Body)
            AppBadge(text = statusLabel(participant.status), tone = statusTone(participant.status))
        }
    }
}

/**
 * Sélecteur d'invitation d'un joueur supplémentaire : liste déroulante des membres pas (ou
 * plus) dans le salon + bouton « Inviter ». Absent quand tout le monde est déjà convié.
 */
@Composable
private fun InviteMoreSection(
    invitable: List<GroupMember>,
    onInvite: (userId: String) -> Unit,
) {
    if (invitable.isEmpty()) return

    var selectedPseudo by remember(invitable) { mutableStateOf<String?>(null) }
    val selectedMember = invitable.firstOrNull { it.pseudo == selectedPseudo }

    AppText(text = "Inviter un autre joueur", style = AppTextStyle.Subtitle)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppDropdown(
            value = selectedPseudo,
            options = invitable.map { it.pseudo },
            onSelect = { selectedPseudo = it },
            label = "Joueur",
            modifier = Modifier.weight(1f),
        )
        AppButton(
            label = "Inviter",
            onClick = {
                selectedMember?.let { onInvite(it.userId) }
                selectedPseudo = null
            },
            leadingIcon = AppIcons.PersonAdd,
            enabled = selectedMember != null,
        )
    }
}

/** Statut d'un joueur ayant décliné l'invitation : il reste reconviable depuis le salon. */
private const val LOBBY_STATUS_REFUSED = "REFUSED"

/** Libellé lisible de l'état d'invitation d'un participant. */
private fun statusLabel(status: String): String = when (status) {
    "INVITED" -> "En attente"
    "ACCEPTED" -> "Présent"
    LOBBY_STATUS_REFUSED -> "A refusé"
    else -> status
}

/** Tonalité de la pastille selon l'état d'invitation. */
private fun statusTone(status: String): BadgeTone = when (status) {
    "ACCEPTED" -> BadgeTone.Accent
    LOBBY_STATUS_REFUSED -> BadgeTone.Danger
    else -> BadgeTone.Neutral
}
