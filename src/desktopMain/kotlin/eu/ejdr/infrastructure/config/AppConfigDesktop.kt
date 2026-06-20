package eu.ejdr.infrastructure.config

import java.io.File

fun provideDataDir(): File {
    val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
    return File(appData, "E-JDR").apply { mkdirs() }
}
