package eu.ejdr.presentation.features.auth.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.features.auth.component.AuthForm

/**
 * Vue interne partagée entre [LoginPage] et [RegisterPage].
 *
 * Composant fin : il observe l'état du [AuthViewModel] (retenu par la destination) et
 * le câble au composant bête [AuthForm]. Tout l'état et le cycle de vie de la coroutine
 * vivent dans le ViewModel ; cette fonction ne détient plus aucun `remember` d'état
 * métier. L'événement one-shot [AuthViewModel.authenticated] déclenche la navigation.
 *
 * @param viewModel ViewModel d'authentification de l'écran (login ou register).
 * @param onAuthenticated Navigation à effectuer une fois authentifié, avec l'utilisateur.
 * @param onSecondaryAction Navigation vers la page complémentaire.
 * @param showPseudo Si vrai (inscription), affiche le champ pseudo dans le formulaire.
 */
@Composable
internal fun AuthPage(
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
