package eu.ejdr.presentation.navigation

sealed interface Screen {
    data object Splash : Screen   // auto-login en cours
    data object Login : Screen
    data object Register : Screen
    data object Home : Screen
}
