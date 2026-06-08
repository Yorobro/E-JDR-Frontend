package eu.ejdr.presentation.feature.auth.page

import androidx.compose.runtime.Composable
import eu.ejdr.application.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.domain.entities.auth.User
import org.koin.compose.koinInject

/**
 * Page d'inscription (composant INTELLIGENT).
 *
 * Injecte [RegisterUseCase] et délègue tout l'état et la logique coroutine à [AuthPage].
 *
 * @param onAuthenticated Callback appelé en cas d'inscription réussie, portant l'utilisateur créé.
 * @param onGoToLogin Callback de navigation vers la page de connexion.
 */
@Composable
fun RegisterPage(
    onAuthenticated: (User) -> Unit,
    onGoToLogin: () -> Unit,
) {
    val registerUseCase = koinInject<RegisterUseCase>()
    AuthPage(
        submit = { credentials -> registerUseCase(credentials) },
        onAuthenticated = onAuthenticated,
        onSecondaryAction = onGoToLogin,
        subtitle = "Créez votre compte pour continuer",
        submitLabel = "S'inscrire",
        secondaryText = "Déjà un compte ?",
        secondaryActionLabel = "Se connecter",
    )
}
