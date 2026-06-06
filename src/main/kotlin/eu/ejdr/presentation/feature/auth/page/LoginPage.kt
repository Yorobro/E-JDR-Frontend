package eu.ejdr.presentation.feature.auth.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import eu.ejdr.application.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.presentation.feature.auth.component.LoginForm
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun LoginPage(
    onAuthenticated: () -> Unit,
    onGoToRegister: () -> Unit,
) {
    val loginUseCase = koinInject<LoginUseCase>()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LoginForm(
        email = email,
        password = password,
        errorMessage = error,
        loading = loading,
        onEmailChange = { email = it; error = null },
        onPasswordChange = { password = it; error = null },
        onGoToRegister = onGoToRegister,
        onSubmit = {
            loading = true
            error = null
            scope.launch {
                when (val result = loginUseCase(Credentials(email.trim(), password))) {
                    is Result.Success -> onAuthenticated()
                    is Result.Failure -> error = result.error.message
                }
                loading = false
            }
        },
    )
}
