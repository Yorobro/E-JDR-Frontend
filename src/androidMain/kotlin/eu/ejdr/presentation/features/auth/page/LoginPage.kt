package eu.ejdr.presentation.features.auth.page

import androidx.compose.runtime.Composable
import eu.ejdr.application.features.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.shared.di.koinViewModel

/**
 * Page de connexion Android (composant intelligent).
 *
 * Crée un [AuthViewModel] retenu par la destination en lui branchant le [LoginUseCase],
 * puis délègue l'affichage à [AuthScreen] (qui réutilise l'organisme commun AuthForm).
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
    AuthScreen(
        viewModel = viewModel,
        onAuthenticated = onAuthenticated,
        onSecondaryAction = onGoToRegister,
        subtitle = "Connectez-vous pour continuer",
        submitLabel = "Se connecter",
        secondaryText = "Pas encore de compte ?",
        secondaryActionLabel = "S'inscrire",
    )
}
