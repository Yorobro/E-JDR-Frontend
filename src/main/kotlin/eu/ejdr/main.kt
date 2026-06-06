package eu.ejdr

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import eu.ejdr.di.initKoin
import eu.ejdr.presentation.App

/**
 * Point d'entrée de l'application desktop.
 *
 * Initialise l'injection de dépendances Koin ([initKoin]) puis lance la fenêtre Compose Desktop
 * qui héberge le composable racine [App].
 */
fun main() {
    initKoin()
    application {
        Window(onCloseRequest = ::exitApplication, title = "E-JDR") {
            App()
        }
    }
}
