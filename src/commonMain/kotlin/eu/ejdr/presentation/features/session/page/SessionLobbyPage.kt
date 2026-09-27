package eu.ejdr.presentation.features.session.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.InviteToLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.RemoveParticipantUseCase
import eu.ejdr.application.features.session.abstraction.usecase.StartSessionUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.features.session.SessionLobbyViewModel
import eu.ejdr.presentation.features.session.component.ConfirmRemoveParticipantDialog
import eu.ejdr.presentation.shared.component.atomic.AppBadge
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.BadgeTone
import eu.ejdr.presentation.shared.component.base.AppIconButton
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
 * refusé (celui-ci reste proposé dans le sélecteur et repasse « En attente » côté serveur) et
 * **retirer** un joueur du salon, après confirmation : la participation est supprimée côté
 * serveur, donc le joueur retiré redevient conviable.
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
            removeParticipant = get<RemoveParticipantUseCase>(),
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

    // Joueur dont le retrait attend confirmation (`null` = pas de dialogue ouvert).
    var pendingRemoval by remember { mutableStateOf<LobbyParticipant?>(null) }

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

        LobbyParticipantList(
            participants = currentLobby.participants,
            pseudoOf = pseudoOf,
            // Seul le MJ peut retirer : pour un joueur, la ligne n'affiche aucun bouton.
            onRemove = if (canManage) ({ participant -> pendingRemoval = participant }) else null,
        )

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

    // Retirer un joueur écarte quelqu'un de la partie : on confirme, comme pour la suppression
    // d'une session. Le message rappelle que le joueur reste reconviable.
    pendingRemoval?.let { target ->
        ConfirmRemoveParticipantDialog(
            pseudo = pseudoOf(target.userId),
            onConfirm = {
                viewModel.remove(target.userId)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }
}

/**
 * Section « Joueurs conviés » : un état vide textuel, ou une ligne par participant.
 *
 * @param participants Participants du lobby, dans l'ordre renvoyé par le serveur.
 * @param pseudoOf Résolution d'un identifiant utilisateur en pseudo affichable.
 * @param onRemove Demande de retrait d'un participant, ou `null` pour masquer le bouton (joueur).
 */
@Composable
private fun LobbyParticipantList(
    participants: List<LobbyParticipant>,
    pseudoOf: (String) -> String,
    onRemove: ((LobbyParticipant) -> Unit)?,
) {
    AppText(text = "Joueurs conviés", style = AppTextStyle.Subtitle)
    if (participants.isEmpty()) {
        AppText(
            text = "Aucun joueur convié pour le moment.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.textSecondary,
        )
        return
    }
    participants.forEach { participant ->
        LobbyParticipantRow(
            pseudo = pseudoOf(participant.userId),
            participant = participant,
            onRemove = onRemove?.let { remove -> { remove(participant) } },
        )
    }
}

/**
 * Ligne d'un participant du lobby : pseudo à gauche, pastille d'état — et bouton de retrait pour
 * le MJ — à droite. Composant bête : il ne connaît pas les rôles, il affiche ce qu'on lui donne
 * ([onRemove] à `null` = pas de bouton), comme `CampaignCard` avec son `onDelete`.
 *
 * Le pseudo prend la place restante et s'ellipse sur une ligne : sur téléphone, un pseudo long ne
 * doit repousser ni la pastille ni le bouton hors de la carte.
 *
 * @param pseudo Pseudo affiché du participant.
 * @param participant Participation (statut d'invitation).
 * @param onRemove Retrait demandé, ou `null` pour ne pas proposer l'action.
 */
@Composable
private fun LobbyParticipantRow(
    pseudo: String,
    participant: LobbyParticipant,
    onRemove: (() -> Unit)?,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = pseudo,
                style = AppTextStyle.Body,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            AppBadge(text = statusLabel(participant.status), tone = statusTone(participant.status))
            if (onRemove != null) {
                AppIconButton(onClick = onRemove, contentDescription = "Retirer ce joueur") {
                    AppIcon(
                        imageVector = AppIcons.Delete,
                        contentDescription = null,
                        tint = AppTheme.colors.danger,
                    )
                }
            }
        }
    }
}

/** En dessous de cette largeur (téléphone), le sélecteur et son bouton s'empilent. */
private val InviteFoldThreshold = 420.dp

/**
 * Sélecteur d'invitation d'un joueur supplémentaire : liste déroulante des membres pas (ou
 * plus) dans le salon + bouton « Inviter ». Absent quand tout le monde est déjà convié.
 *
 * Responsive comme le reste de l'app (cf. `ResponsiveColumns` de la fiche) : côte à côte au
 * large, empilés sous [InviteFoldThreshold] pour rester utilisables sur téléphone.
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < InviteFoldThreshold
        val dropdown: @Composable (Modifier) -> Unit = { modifier ->
            AppDropdown(
                value = selectedPseudo,
                options = invitable.map { it.pseudo },
                onSelect = { selectedPseudo = it },
                label = "Joueur",
                modifier = modifier,
            )
        }
        val button: @Composable (Modifier) -> Unit = { modifier ->
            AppButton(
                label = "Inviter",
                onClick = {
                    selectedMember?.let { onInvite(it.userId) }
                    selectedPseudo = null
                },
                leadingIcon = AppIcons.PersonAdd,
                enabled = selectedMember != null,
                modifier = modifier,
            )
        }

        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                dropdown(Modifier.fillMaxWidth())
                button(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dropdown(Modifier.weight(1f))
                button(Modifier)
            }
        }
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
