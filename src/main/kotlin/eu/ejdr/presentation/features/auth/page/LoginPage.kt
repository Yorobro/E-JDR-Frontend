package eu.ejdr.presentation.features.auth.page

import androidx.compose.runtime.Composable
import eu.ejdr.application.features.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.domain.features.auth.entities.User
import org.koin.compose.koinInject

/**
 * Page de connexion (composant INTELLIGENT).
 *
 * Injecte [LoginUseCase] et délègue tout l'état et la logique coroutine à [AuthPage].
 *
 * @param onAuthenticated Callback appelé en cas de connexion réussie, portant l'utilisateur connecté.
 * @param onGoToRegister Callback de navigation vers la page d'inscription.
 */
@Composable
fun LoginPage(
    onAuthenticated: (User) -> Unit,
    onGoToRegister: () -> Unit,
) {
    val loginUseCase = koinInject<LoginUseCase>()
    AuthPage(
        submit = { credentials -> loginUseCase(credentials) },
        onAuthenticated = onAuthenticated,
        onSecondaryAction = onGoToRegister,
        subtitle = "Connectez-vous pour continuer",
        submitLabel = "Se connecter",
        secondaryText = "Pas encore de compte ?",
        secondaryActionLabel = "S'inscrire",
    )
}
