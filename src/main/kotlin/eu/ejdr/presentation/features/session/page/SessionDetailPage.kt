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
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.presentation.features.session.SessionDetailViewModel
import eu.ejdr.presentation.features.session.component.ConfirmDeleteSessionDialog
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/** Motif strict d'une date `YYYY-MM-DD` (validation côté UI ; le serveur revalide). */
private val DatePattern = Regex("""\d{4}-\d{2}-\d{2}""")

/**
 * Page détail d'une session (composant INTELLIGENT), éditable.
 *
 * Charge la session par son identifiant, pré-remplit les champs titre + date, et permet de
 * sauvegarder ou de supprimer. Après suppression, déclenche [onDeleted] pour revenir à la
 * campagne.
 *
 * @param id Identifiant de la session.
 * @param title Titre initial (affiché immédiatement en attendant le chargement complet).
 * @param onDeleted Callback déclenché après une suppression réussie (retour arrière).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun SessionDetailPage(
    id: String,
    title: String,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        SessionDetailViewModel(
            sessionId = id,
            getById = get<GetSessionUseCase>(),
            update = get<UpdateSessionUseCase>(),
            deleteSession = get<DeleteSessionUseCase>(),
        )
    }
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    // Champs éditables, initialisés depuis la session une fois chargée.
    var titleField by remember { mutableStateOf(title) }
    var dateField by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(session) {
        session?.let {
            titleField = it.title
            dateField = it.date
        }
    }

    LaunchedEffect(deleted) {
        if (deleted) onDeleted()
    }

    val dateValid = DatePattern.matches(dateField)
    val canSave = titleField.isNotBlank() && dateValid && !isLoading

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = "Session", style = AppTextStyle.Title)

        AppTextField(
            value = titleField,
            onValueChange = { titleField = it },
            label = "Titre de la session",
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = dateField,
            onValueChange = { dateField = it },
            label = "Date (AAAA-MM-JJ)",
            placeholder = "2026-06-20",
            errorMessage = if (dateField.isNotBlank() && !dateValid) "Format attendu : AAAA-MM-JJ" else null,
            modifier = Modifier.fillMaxWidth(),
        )

        FormError(message = error)

        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md)) {
            AppButton(
                label = "Enregistrer",
                onClick = { viewModel.save(titleField.trim(), dateField.trim()) },
                enabled = canSave,
                loading = isLoading,
            )
            AppButton(
                label = "Supprimer",
                onClick = { showDelete = true },
                variant = ButtonVariant.Danger,
                enabled = !isLoading,
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
}
