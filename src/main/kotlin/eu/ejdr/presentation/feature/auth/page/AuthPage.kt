package eu.ejdr.presentation.feature.auth.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.DomainError
import eu.ejdr.presentation.feature.auth.component.AuthForm
import kotlinx.coroutines.launch

/**
 * Composant interne partagé entre [LoginPage] et [RegisterPage].
 *
 * Gère l'état du formulaire (email, mot de passe, erreur, chargement) et le
 * cycle de vie de la coroutine. La logique métier est injectée via [submit], ce
 * qui permet à chaque page de brancher son propre use case sans dupliquer l'état.
 *
 * @param submit Appel au use case métier avec les identifiants saisis.
 * @param onAuthenticated Callback appelé en cas de succès, portant l'utilisateur.
 * @param onSecondaryAction Callback de navigation vers la page complémentaire.
 */
@Composable
internal fun AuthPage(
    submit: suspend (Credentials) -> Result<User, out DomainError>,
    onAuthenticated: (User) -> Unit,
    onSecondaryAction: () -> Unit,
    subtitle: String,
    submitLabel: String,
    secondaryText: String,
    secondaryActionLabel: String,
) {
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
        onSecondaryAction = onSecondaryAction,
        subtitle = subtitle,
        submitLabel = submitLabel,
        secondaryText = secondaryText,
        secondaryActionLabel = secondaryActionLabel,
        onSubmit = {
            loading = true
            error = null
            scope.launch {
                when (val result = submit(Credentials(email.trim(), password))) {
                    is Result.Success -> onAuthenticated(result.value)
                    is Result.Failure -> error = result.error.message
                }
                loading = false
            }
        },
    )
}
