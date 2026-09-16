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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.shared.component.atomic.AppBadge
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppDropdown
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.BadgeTone
import eu.ejdr.presentation.shared.component.molecule.EmptyState
import eu.ejdr.presentation.shared.component.organism.AppCard
import eu.ejdr.presentation.shared.component.organism.PageHeader
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Salon d'attente (lobby) d'une session — écran INTELLIGENT réservé au MJ.
 *
 * Observe le [SessionLobbyState] partagé (alimenté par le détail de session au moment du
 * `launch`) : liste les joueurs conviés et l'état de leur invitation, permet d'en convier
 * d'autres (refus « accidentel » ou oubli), puis de démarrer réellement la session.
 *
 * Les réponses aux invitations arriveront à terme en temps réel (WebSocket) via le même
 * [SessionLobbyState] ; l'écran se recomposera alors sans changement. Si l'état est vide
 * (ex. lobby non ouvert, reprise à froid), un état vide invite à repasser par le détail.
 *
 * @param title Titre de la session (affiché en en-tête).
 * @param onStarted Callback déclenché quand le MJ démarre la session (câblage de l'écran de jeu à venir).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun SessionLobbyPage(
    title: String,
    onStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lobbyState = koinInject<SessionLobbyState>()
    val lobby by lobbyState.lobby.collectAsStateWithLifecycle()
    val members by lobbyState.members.collectAsStateWithLifecycle()

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

    // Joueurs déjà conviés (résolus vers leur pseudo) et joueurs encore conviables.
    val invitedIds = currentLobby.participants.map { it.userId }.toSet()
    val invitable = members.filter { it.userId !in invitedIds }
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

        InviteMoreSection(
            invitable = invitable,
            onInvite = { userId -> lobbyState.invite(userId) },
        )

        AppButton(
            label = "Commencer la session",
            onClick = {
                lobbyState.startSession()
                onStarted()
            },
            leadingIcon = AppIcons.Play,
            modifier = Modifier.fillMaxWidth(),
        )
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
 * Sélecteur d'invitation d'un joueur supplémentaire : liste déroulante des membres non conviés
 * + bouton « Inviter ». Absent quand tout le monde est déjà convié.
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

/** Libellé lisible de l'état d'invitation d'un participant. */
private fun statusLabel(status: String): String = when (status) {
    "INVITED" -> "En attente"
    "ACCEPTED" -> "Présent"
    "REFUSED" -> "A refusé"
    else -> status
}

/** Tonalité de la pastille selon l'état d'invitation. */
private fun statusTone(status: String): BadgeTone = when (status) {
    "ACCEPTED" -> BadgeTone.Accent
    "REFUSED" -> BadgeTone.Danger
    else -> BadgeTone.Neutral
}
