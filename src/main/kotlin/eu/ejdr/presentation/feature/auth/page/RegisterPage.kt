package eu.ejdr.presentation.feature.auth.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import eu.ejdr.application.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.presentation.feature.auth.component.AuthForm
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Page d'inscription (composant INTELLIGENT).
 *
 * Seul endroit de la feature auth qui injecte et appelle le [RegisterUseCase] (via [koinInject]).
 * Elle détient l'état local du formulaire (email, mot de passe, erreur, chargement), orchestre
 * l'appel au use case dans une coroutine, et traduit l'erreur domaine ([Result.Failure]) en
 * message UI. Le rendu est délégué au composant bête [AuthForm].
 *
 * @param onAuthenticated Callback appelé en cas d'inscription réussie, portant l'utilisateur connecté.
 * @param onGoToLogin Callback de navigation vers la page de connexion.
 */
@Composable
fun RegisterPage(
    onAuthenticated: (User) -> Unit,
    onGoToLogin: () -> Unit,
) {
    val registerUseCase = koinInject<RegisterUseCase>()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    AuthForm(
        email = email,
        password = password,
        errorMessage = error,
        loading = loading,
        onEmailChange = { email = it; error = null },
        onPasswordChange = { password = it; error = null },
        onSecondaryAction = onGoToLogin,
        subtitle = "Créez votre compte pour continuer",
        submitLabel = "S'inscrire",
        secondaryText = "Déjà un compte ?",
        secondaryActionLabel = "Se connecter",
        onSubmit = {
            loading = true
            error = null
            scope.launch {
                when (val result = registerUseCase(Credentials(email.trim(), password))) {
                    is Result.Success -> onAuthenticated(result.value)
                    is Result.Failure -> error = result.error.message
                }
                loading = false
            }
        },
    )
}
