package eu.ejdr.presentation.features.campaign.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.presentation.features.campaign.CampaignDetailViewModel
import eu.ejdr.presentation.features.campaign.component.LinkCharacterDialog
import eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCard
import eu.ejdr.presentation.features.session.component.CreateSessionDialog
import eu.ejdr.presentation.features.session.component.SessionCard
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page détail d'une campagne (composant INTELLIGENT).
 *
 * Affiche le nom de la campagne, la liste des fiches rattachées (avec détachement et
 * rattachement par le MJ), puis une section **Sessions** : les sessions de la campagne sous
 * forme de cartes cliquables (ouvrant leur détail via [onOpenSession]) et un bouton d'ajout.
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
    val viewModel = koinViewModel {
        CampaignDetailViewModel(
            campaignId = id,
            listCampaignCharacters = get<ListCampaignCharactersUseCase>(),
            listLinkable = get<ListLinkableCharactersUseCase>(),
            linkCharacter = get<LinkCharacterToCampaignUseCase>(),
            unlinkCharacter = get<UnlinkCharacterFromCampaignUseCase>(),
            listCampaignSessions = get<ListCampaignSessionsUseCase>(),
            createSession = get<CreateSessionUseCase>(),
        )
    }
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val linkableSheets by viewModel.linkableSheets.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showLink by remember { mutableStateOf(false) }
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

        CharactersSection(
            characters = characters,
            onLinkRequest = { showLink = true },
            onUnlink = viewModel::unlink,
        )

        SessionsSection(
            sessions = sessions,
            onAddRequest = { showCreateSession = true },
            onOpenSession = onOpenSession,
        )
    }

    if (showLink) {
        // Les fiches rattachables sont déjà filtrées côté back (autres joueurs, hors déjà liées).
        LinkCharacterDialog(
            sheets = linkableSheets,
            onSelect = { sheetId ->
                showLink = false
                viewModel.link(sheetId)
            },
            onDismiss = { showLink = false },
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

/** Section « Personnages » : bouton de rattachement + liste des fiches rattachées. */
@Composable
private fun CharactersSection(
    characters: List<CharacterSheet>,
    onLinkRequest: () -> Unit,
    onUnlink: (String) -> Unit,
) {
    AppText(
        text = "Personnages",
        style = AppTextStyle.Subtitle,
        color = AppTheme.colors.textSecondary,
    )
    AppButton(
        label = "Rattacher une fiche",
        onClick = onLinkRequest,
        leadingIcon = Icons.Filled.Add,
    )
    if (characters.isEmpty()) {
        AppText(
            text = "Aucune fiche rattachée.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.muted,
        )
    } else {
        characters.forEach { sheet ->
            CharacterSheetCard(
                sheet = sheet,
                onDelete = { onUnlink(sheet.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
    onAddRequest: () -> Unit,
    onOpenSession: (id: String, title: String) -> Unit,
) {
    AppText(
        text = "Sessions",
        style = AppTextStyle.Subtitle,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(top = AppTheme.dimens.md),
    )
    AppButton(
        label = "Ajouter une session",
        onClick = onAddRequest,
        leadingIcon = Icons.Filled.Add,
    )
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
