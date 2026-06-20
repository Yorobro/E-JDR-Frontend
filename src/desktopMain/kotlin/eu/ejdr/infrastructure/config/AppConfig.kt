package eu.ejdr.infrastructure.config

import eu.ejdr.BuildConfig
import java.io.File

/**
 * Résolution desktop de [AppConfig] : calcule le `dataDir` depuis APPDATA (Windows)
 * ou le répertoire home de l'utilisateur (autres OS), puis construit un [AppConfig].
 */
fun AppConfig.Companion.load(): AppConfig {
    val appData = System.getenv("APPDATA")
        ?: System.getProperty("user.home")
    val dataDir = File(appData, "E-JDR").apply { mkdirs() }
    return AppConfig(
        baseUrl = BuildConfig.API_URL,
        dataDir = dataDir.absolutePath,
        enableHttpLogging = BuildConfig.HTTP_LOGGING,
    )
}
