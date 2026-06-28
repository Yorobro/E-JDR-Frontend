package eu.ejdr.presentation.features.campaign.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.AcceptCharacterUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListPendingCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.RefuseCharacterUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.presentation.features.campaign.CampaignDetailViewModel
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCard
import eu.ejdr.presentation.features.session.component.CreateSessionDialog
import eu.ejdr.presentation.features.session.component.SessionCard
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Page détail d'une campagne (composant INTELLIGENT).
 *
 * Affiche le nom de la campagne, la liste des fiches acceptées (lecture seule), une section
 * **Demandes en attente** où le MJ valide/refuse les rattachements PENDING, puis une section
 * **Sessions** : les sessions de la campagne sous forme de cartes cliquables (ouvrant leur détail
 * via [onOpenSession]) et un bouton d'ajout.
 *
 * @param id Identifiant de la campagne.
 * @param name Nom de la campagne (affiché en titre).
 * @param onOpenSession Callback d'ouverture du détail d'une session (id + titre).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun CampaignDetailPage(
    id: String,
    name: String,
    onOpenSession: (id: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeGroupState = koinInject<ActiveGroupState>()
    val viewModel = koinViewModel {
        CampaignDetailViewModel(
            campaignId = id,
            activeGroupId = activeGroupState.activeGroupId,
            listCampaignCharacters = get<ListCampaignCharactersUseCase>(),
            listPendingCharacters = get<ListPendingCharactersUseCase>(),
            acceptCharacter = get<AcceptCharacterUseCase>(),
            refuseCharacter = get<RefuseCharacterUseCase>(),
            listCampaignSessions = get<ListCampaignSessionsUseCase>(),
            createSession = get<CreateSessionUseCase>(),
            listCampaigns = get<ListCampaignsUseCase>(),
            getCurrentUser = get<GetCurrentUserUseCase>(),
            uiMessageBus = get<UiMessageBus>(),
        )
    }
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val pendingCharacters by viewModel.pendingCharacters.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val isGameMaster by viewModel.isGameMaster.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showCreateSession by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = name, style = AppTextStyle.Title)
        FormError(message = error)

        CharactersSection(characters = characters)

        if (isGameMaster) {
            PendingRequestsSection(
                pending = pendingCharacters,
                onAccept = viewModel::accept,
                onRefuse = viewModel::refuse,
            )
        }

        SessionsSection(
            sessions = sessions,
            onAddRequest = if (isGameMaster) ({ showCreateSession = true }) else null,
            onOpenSession = onOpenSession,
        )
    }

    if (showCreateSession) {
        CreateSessionDialog(
            onDismiss = { showCreateSession = false },
            onConfirm = { title, date ->
                showCreateSession = false
                viewModel.createSession(title, date)
            },
        )
    }
}

/** Section « Personnages » : liste des fiches acceptées (lecture seule). */
@Composable
private fun CharactersSection(characters: List<CharacterSheet>) {
    AppText(
        text = "Personnages",
        style = AppTextStyle.Subtitle,
        color = AppTheme.colors.textSecondary,
    )
    if (characters.isEmpty()) {
        AppText(
            text = "Aucune fiche rattachée.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.muted,
        )
    } else {
        characters.forEach { sheet ->
            CharacterSheetCard(sheet = sheet, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Section « Demandes en attente » (MJ) : pour chaque demande PENDING, le nom de la fiche et deux
 * actions « Accepter » / « Refuser ».
 */
@Composable
private fun PendingRequestsSection(
    pending: List<CharacterSheet>,
    onAccept: (String) -> Unit,
    onRefuse: (String) -> Unit,
) {
    AppText(
        text = "Demandes en attente",
        style = AppTextStyle.Subtitle,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(top = AppTheme.dimens.md),
    )
    if (pending.isEmpty()) {
        AppText(
            text = "Aucune demande en attente.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.muted,
        )
    } else {
        pending.forEach { sheet ->
            PendingRequestRow(sheet = sheet, onAccept = onAccept, onRefuse = onRefuse)
        }
    }
}

/** Une ligne de demande en attente : nom de la fiche + boutons Accepter/Refuser. */
@Composable
private fun PendingRequestRow(
    sheet: CharacterSheet,
    onAccept: (String) -> Unit,
    onRefuse: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(text = sheet.name, style = AppTextStyle.Body, modifier = Modifier.weight(1f))
        AppButton(label = "Accepter", onClick = { onAccept(sheet.id) })
        AppButton(
            label = "Refuser",
            onClick = { onRefuse(sheet.id) },
            variant = ButtonVariant.Danger,
        )
    }
}

/**
 * Section « Sessions » : bouton d'ajout + liste des sessions (cartes cliquables).
 *
 * Rendu direct (pas de LazyColumn) : la page entière défile déjà via `verticalScroll`, et
 * imbriquer un conteneur vertical défilant provoquerait une contrainte de hauteur infinie.
 */
@Composable
private fun SessionsSection(
    sessions: List<Session>,
    onAddRequest: (() -> Unit)?,
    onOpenSession: (id: String, title: String) -> Unit,
) {
    AppText(
        text = "Sessions",
        style = AppTextStyle.Subtitle,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(top = AppTheme.dimens.md),
    )
    if (onAddRequest != null) {
        AppButton(
            label = "Ajouter une session",
            onClick = onAddRequest,
            leadingIcon = Icons.Filled.Add,
        )
    }
    if (sessions.isEmpty()) {
        AppText(
            text = "Aucune session pour le moment.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.muted,
        )
    } else {
        sessions.forEach { session ->
            SessionCard(
                session = session,
                onClick = { onOpenSession(session.id, session.title) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
