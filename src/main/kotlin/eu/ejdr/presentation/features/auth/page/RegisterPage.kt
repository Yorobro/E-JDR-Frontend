package eu.ejdr.presentation.features.auth.page

import androidx.compose.runtime.Composable
import eu.ejdr.application.features.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.shared.di.koinViewModel

/**
 * Page d'inscription (composant INTELLIGENT).
 *
 * Crée un [AuthViewModel] **retenu par la destination** (via `viewModel { }`) en lui
 * branchant le [RegisterUseCase], puis délègue l'affichage à [AuthPage].
 *
 * @param onAuthenticated Callback appelé en cas d'inscription réussie, portant l'utilisateur créé.
 * @param onGoToLogin Callback de navigation vers la page de connexion.
 */
@Composable
fun RegisterPage(
    onAuthenticated: (User) -> Unit,
    onGoToLogin: () -> Unit,
) {
    val viewModel = koinViewModel {
        val registerUseCase = get<RegisterUseCase>()
        AuthViewModel(submit = { credentials -> registerUseCase(credentials) })
    }
    AuthPage(
        viewModel = viewModel,
        onAuthenticated = onAuthenticated,
        onSecondaryAction = onGoToLogin,
        subtitle = "Créez votre compte pour continuer",
        submitLabel = "S'inscrire",
        secondaryText = "Déjà un compte ?",
        secondaryActionLabel = "Se connecter",
    )
}
