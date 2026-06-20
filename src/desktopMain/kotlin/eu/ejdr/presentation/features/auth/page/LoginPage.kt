package eu.ejdr.presentation.features.auth.page

import androidx.compose.runtime.Composable
import eu.ejdr.application.features.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.shared.di.koinViewModel

/**
 * Page de connexion (composant INTELLIGENT).
 *
 * Crée un [AuthViewModel] **retenu par la destination** (via `viewModel { }`) en lui
 * branchant le [LoginUseCase], puis délègue l'affichage à [AuthPage]. Le use case est
 * résolu par Koin et passé au ViewModel ; le reste de la logique vit dans ce dernier.
 *
 * @param onAuthenticated Callback appelé en cas de connexion réussie, portant l'utilisateur connecté.
 * @param onGoToRegister Callback de navigation vers la page d'inscription.
 */
@Composable
fun LoginPage(
    onAuthenticated: (User) -> Unit,
    onGoToRegister: () -> Unit,
) {
    val viewModel = koinViewModel {
        val loginUseCase = get<LoginUseCase>()
        AuthViewModel(submit = { credentials -> loginUseCase(credentials) })
    }
    AuthPage(
        viewModel = viewModel,
        onAuthenticated = onAuthenticated,
        onSecondaryAction = onGoToRegister,
        subtitle = "Connectez-vous pour continuer",
        submitLabel = "Se connecter",
        secondaryText = "Pas encore de compte ?",
        secondaryActionLabel = "S'inscrire",
    )
}
