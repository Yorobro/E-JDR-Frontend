package eu.ejdr.presentation.features.user.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import eu.ejdr.presentation.shared.component.atomic.AppDivider
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.theme.AppTreatment
import eu.ejdr.presentation.shared.theme.ProvideTreatment
import eu.ejdr.application.features.auth.abstraction.usecase.ChangeEmailUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.ChangePasswordUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.domain.features.auth.entities.User
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
 * Page profil Android affichée une fois connecté (composant intelligent).
 *
 * Reprend la logique de la page desktop (mêmes ViewModel, dialogs communs et atoms) ; seul le
 * layout est adapté au mobile : carte profil en pleine largeur plutôt que largeur fixe.
 *
 * @param onSessionExpired Callback de retour à l'écran de connexion (session expirée).
 * @param onLogout Callback de déconnexion volontaire.
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

    LaunchedEffect(viewModel) {
        viewModel.sessionExpired.collect { onSessionExpired() }
    }

    ProvideTreatment(AppTreatment.Rich) {
        ProfileDialogs(
            editState = editState,
            onEmailConfirm = viewModel::changeEmail,
            onPasswordConfirm = viewModel::changePassword,
            onResetEditState = viewModel::resetEditState,
        ) { onChangeEmail, onChangePassword ->
            ProfileCard(
                profile = profile,
                onChangeEmail = onChangeEmail,
                onChangePassword = onChangePassword,
                onLogout = onLogout,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ProfileDialogs(
    editState: EditState,
    onEmailConfirm: (String) -> Unit,
    onPasswordConfirm: (String, String) -> Unit,
    onResetEditState: () -> Unit,
    content: @Composable (onChangeEmail: () -> Unit, onChangePassword: () -> Unit) -> Unit,
) {
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editState) {
        if (editState is EditState.Success) {
            showEmailDialog = false
            showPasswordDialog = false
            onResetEditState()
        }
    }

    if (showEmailDialog) {
        ChangeEmailDialog(
            onDismiss = { showEmailDialog = false; onResetEditState() },
            onConfirm = onEmailConfirm,
            errorMessage = (editState as? EditState.Error)?.message,
        )
    }
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false; onResetEditState() },
            onConfirm = onPasswordConfirm,
            errorMessage = (editState as? EditState.Error)?.message,
        )
    }

    content(
        { showEmailDialog = true },
        { showPasswordDialog = true },
    )
}

@Composable
private fun ProfileCard(
    profile: User?,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
            color = AppTheme.colors.surface,
            elevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.dimens.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
            ) {
                AppText(text = "Mon profil", style = AppTextStyle.Title)
                profile?.let { current ->
                    AppText(text = current.pseudo, style = AppTextStyle.Subtitle)
                    AppText(text = current.email, style = AppTextStyle.Body, color = AppTheme.colors.textSecondary)
                }
                AppDivider(modifier = Modifier.padding(vertical = AppTheme.dimens.xs))
                ProfileActions(onChangeEmail = onChangeEmail, onChangePassword = onChangePassword, onLogout = onLogout)
            }
        }
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
        AppButton(label = "Changer d'email", onClick = onChangeEmail, modifier = Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
        AppButton(label = "Changer le mot de passe", onClick = onChangePassword, modifier = Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
        AppButton(label = "Déconnexion", onClick = onLogout, modifier = Modifier.fillMaxWidth(), variant = ButtonVariant.Danger)
    }
}
