package eu.ejdr.presentation.features.session.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import eu.ejdr.presentation.features.session.SessionDetailViewModel
import eu.ejdr.presentation.features.session.SessionLobbyState
import eu.ejdr.presentation.features.session.component.ConfirmDeleteSessionDialog
import eu.ejdr.presentation.features.session.component.LaunchSessionDialog
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

private val DatePattern = Regex("""\d{4}-\d{2}-\d{2}""")

/** Détail d'une session (Android), éditable : titre + date, enregistrer / supprimer. */
@Composable
fun SessionDetailPage(
    id: String,
    title: String,
    onDeleted: () -> Unit,
    onLobbyOpened: (id: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeGroupState = koinInject<ActiveGroupState>()
    val viewModel = koinViewModel {
        SessionDetailViewModel(
            sessionId = id,
            activeGroupId = activeGroupState.activeGroupId,
            getById = get<GetSessionUseCase>(),
            update = get<UpdateSessionUseCase>(),
            deleteSession = get<DeleteSessionUseCase>(),
            createLobby = get<CreateLobbyUseCase>(),
            getGroup = get<GetGroupUseCase>(),
            getCurrentUser = get<GetCurrentUserUseCase>(),
            listCampaigns = get<ListCampaignsUseCase>(),
            uiMessageBus = get<UiMessageBus>(),
            lobbyState = get<SessionLobbyState>(),
        )
    }
    val activeGroupId by activeGroupState.activeGroupId.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val lobbyOpened by viewModel.lobbyOpened.collectAsStateWithLifecycle()
    val isGameMaster by viewModel.isGameMaster.collectAsStateWithLifecycle()
    val selectableMembers by viewModel.selectableMembers.collectAsStateWithLifecycle()

    var titleField by remember { mutableStateOf(title) }
    var dateField by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var showLaunch by remember { mutableStateOf(false) }

    LaunchedEffect(session) {
        session?.let {
            titleField = it.title
            dateField = it.date
        }
    }

    LaunchedEffect(deleted) {
        if (deleted) onDeleted()
    }

    LaunchedEffect(lobbyOpened) {
        if (lobbyOpened) {
            onLobbyOpened(id, session?.title ?: titleField)
            viewModel.consumeLobbyOpened()
        }
    }

    val dateValid = DatePattern.matches(dateField)
    val canSave = titleField.isNotBlank() && dateValid && !isLoading

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = "Session", style = AppTextStyle.Title)

        AppTextField(
            value = titleField,
            onValueChange = { titleField = it },
            label = "Titre de la session",
            enabled = isGameMaster,
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = dateField,
            onValueChange = { dateField = it },
            label = "Date (AAAA-MM-JJ)",
            placeholder = "2026-06-20",
            enabled = isGameMaster,
            errorMessage = if (dateField.isNotBlank() && !dateValid) "Format attendu : AAAA-MM-JJ" else null,
            modifier = Modifier.fillMaxWidth(),
        )

        FormError(message = error)

        if (isGameMaster) {
            if (session?.status == "PLANNED") {
                AppButton(
                    label = "Lancer la session",
                    onClick = {
                        activeGroupId?.let { viewModel.loadSelectableMembers(it) }
                        showLaunch = true
                    },
                    enabled = !isLoading,
                )
            }
            SessionEditActions(
                canSave = canSave,
                isLoading = isLoading,
                onSave = { viewModel.save(titleField.trim(), dateField.trim()) },
                onDeleteRequest = { showDelete = true },
            )
        }
    }

    if (showDelete) {
        ConfirmDeleteSessionDialog(
            sessionTitle = titleField,
            onConfirm = {
                showDelete = false
                viewModel.delete()
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showLaunch) {
        LaunchSessionDialog(
            members = selectableMembers,
            loading = isLoading,
            onDismiss = { showLaunch = false },
            onConfirm = { ids ->
                showLaunch = false
                viewModel.openLobby(ids)
            },
        )
    }
}

@Composable
private fun SessionEditActions(
    canSave: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md)) {
        AppButton(
            label = "Enregistrer",
            onClick = onSave,
            enabled = canSave,
            loading = isLoading,
        )
        AppButton(
            label = "Supprimer",
            onClick = onDeleteRequest,
            variant = ButtonVariant.Danger,
            enabled = !isLoading,
        )
    }
}
