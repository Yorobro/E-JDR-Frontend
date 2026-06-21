package eu.ejdr.presentation.features.auth.page

import androidx.compose.runtime.Composable
import eu.ejdr.application.features.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.shared.di.koinViewModel

/**
 * Page d'inscription Android (composant intelligent).
 *
 * Crée un [AuthViewModel] retenu par la destination en lui branchant le [RegisterUseCase]
 * (pseudo requis), puis délègue l'affichage à [AuthScreen].
 */
@Composable
fun RegisterPage(
    onAuthenticated: (User) -> Unit,
    onGoToLogin: () -> Unit,
) {
    val viewModel = koinViewModel {
        val registerUseCase = get<RegisterUseCase>()
        AuthViewModel(submit = { credentials -> registerUseCase(credentials) }, requirePseudo = true)
    }
    AuthScreen(
        viewModel = viewModel,
        onAuthenticated = onAuthenticated,
        onSecondaryAction = onGoToLogin,
        subtitle = "Créez votre compte pour continuer",
        submitLabel = "S'inscrire",
        secondaryText = "Déjà un compte ?",
        secondaryActionLabel = "Se connecter",
        showPseudo = true,
    )
}
