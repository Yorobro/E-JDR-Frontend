package eu.ejdr.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.application.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.presentation.feature.auth.page.LoginPage
import eu.ejdr.presentation.feature.auth.page.RegisterPage
import eu.ejdr.presentation.navigation.Screen
import org.koin.compose.koinInject

@Composable
fun App() {
    MaterialTheme {
        val restoreSession = koinInject<RestoreSessionUseCase>()
        var screen by remember { mutableStateOf<Screen>(Screen.Splash) }

        LaunchedEffect(Unit) {
            screen = when (restoreSession()) {
                is Result.Success -> Screen.Home
                is Result.Failure -> Screen.Login
            }
        }

        when (screen) {
            Screen.Splash -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            Screen.Login -> LoginPage(
                onAuthenticated = { screen = Screen.Home },
                onGoToRegister = { screen = Screen.Register },
            )
            Screen.Register -> RegisterPage(
                onAuthenticated = { screen = Screen.Home },
                onGoToLogin = { screen = Screen.Login },
            )
            Screen.Home -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Bienvenue sur E-JDR") }
        }
    }
}
