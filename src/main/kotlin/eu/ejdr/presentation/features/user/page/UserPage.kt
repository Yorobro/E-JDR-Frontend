package eu.ejdr.presentation.features.user.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.auth.abstraction.usecase.ChangeEmailUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.ChangePasswordUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.presentation.features.user.EditState
import eu.ejdr.presentation.features.user.UserViewModel
import eu.ejdr.presentation.features.user.component.ChangeEmailDialog
import eu.ejdr.presentation.features.user.component.ChangePasswordDialog
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page profil affichée une fois l'utilisateur connecté (composant intelligent).
 *
 * Crée un [UserViewModel] retenu par la destination et observe son état. Expose des actions
 * de changement d'email et de mot de passe via des boîtes de dialogue, et un bouton de
 * déconnexion. L'événement one-shot [UserViewModel.sessionExpired] déclenche [onSessionExpired].
 *
 * @param onSessionExpired Callback de retour à l'écran de connexion (session expirée).
 * @param onLogout Callback de déconnexion volontaire.
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun UserPage(
    onSessionExpired: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        UserViewModel(get<GetCurrentUserUseCase>(), get<ChangeEmailUseCase>(), get<ChangePasswordUseCase>())
    }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()

    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.sessionExpired.collect { onSessionExpired() }
    }

    LaunchedEffect(editState) {
        if (editState is EditState.Success) {
            showEmailDialog = false
            showPasswordDialog = false
            viewModel.resetEditState()
        }
    }

    if (showEmailDialog) {
        ChangeEmailDialog(
            onDismiss = {
                showEmailDialog = false
                viewModel.resetEditState()
            },
            onConfirm = { newEmail -> viewModel.changeEmail(newEmail) },
            errorMessage = (editState as? EditState.Error)?.message,
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = {
                showPasswordDialog = false
                viewModel.resetEditState()
            },
            onConfirm = { current, new -> viewModel.changePassword(current, new) },
            errorMessage = (editState as? EditState.Error)?.message,
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm, Alignment.CenterVertically),
    ) {
        AppText(text = "Mon profil", style = AppTextStyle.Title)
        profile?.let { current ->
            AppText(
                text = current.email,
                style = AppTextStyle.Body,
                color = AppTheme.colors.textSecondary,
            )
        }
        ProfileActions(
            onChangeEmail = { showEmailDialog = true },
            onChangePassword = { showPasswordDialog = true },
            onLogout = onLogout,
        )
    }
}

@Composable
private fun ProfileActions(
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
    ) {
        AppButton(
            label = "Changer d'email",
            onClick = onChangeEmail,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary,
        )
        AppButton(
            label = "Changer le mot de passe",
            onClick = onChangePassword,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary,
        )
        AppButton(
            label = "Déconnexion",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Danger,
        )
    }
}
