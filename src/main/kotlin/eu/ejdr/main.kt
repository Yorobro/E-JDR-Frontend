package eu.ejdr

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import eu.ejdr.di.initKoin
import eu.ejdr.presentation.App

fun main() {
    initKoin()
    application {
        Window(onCloseRequest = ::exitApplication, title = "E-JDR") {
            App()
        }
    }
}
