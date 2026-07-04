package eu.ejdr.presentation.features.auth.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.features.auth.component.AuthForm

/**
 * Vue interne partagée entre [LoginPage] et [RegisterPage] (Android).
 *
 * Équivalent mobile de l'`AuthPage` desktop : observe l'état du [AuthViewModel] retenu par la
 * destination et le câble à l'organisme **commun** [AuthForm] (rendu identique au desktop).
 * L'événement one-shot [AuthViewModel.authenticated] déclenche la navigation.
 */
@Composable
internal fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: (User) -> Unit,
    onSecondaryAction: () -> Unit,
    subtitle: String,
    submitLabel: String,
    secondaryText: String,
    secondaryActionLabel: String,
    showPseudo: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.authenticated.collect(onAuthenticated)
    }

    AuthForm(
        email = state.email,
        password = state.password,
        pseudo = state.pseudo,
        showPseudo = showPseudo,
        errorMessage = state.error,
        loading = state.loading,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPseudoChange = viewModel::onPseudoChange,
        onSubmit = viewModel::onSubmit,
        onSecondaryAction = onSecondaryAction,
        subtitle = subtitle,
        submitLabel = submitLabel,
        secondaryText = secondaryText,
        secondaryActionLabel = secondaryActionLabel,
    )
}
