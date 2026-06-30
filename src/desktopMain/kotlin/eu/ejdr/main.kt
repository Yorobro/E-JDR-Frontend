package eu.ejdr

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import eu.ejdr.di.initKoin
import eu.ejdr.presentation.App

/**
 * Point d'entrée de l'application desktop.
 *
 * Initialise l'injection de dépendances Koin ([initKoin]) puis lance la fenêtre Compose Desktop
 * qui héberge le composable racine [App]. La fenêtre a une taille initiale confortable adaptée
 * à la grille de tuiles et à la fiche de personnage (la valeur par défaut ~800×600 était à l'étroit).
 */
fun main() {
    initKoin()
    application {
        val windowState = rememberWindowState(size = DpSize(1100.dp, 740.dp))
        Window(onCloseRequest = ::exitApplication, state = windowState, title = "E-JDR") {
            App()
        }
    }
}
