package eu.ejdr.infrastructure.config

import java.io.File

/**
 * Config applicative. baseUrl lisible via variable d'env EJDR_API_URL,
 * sinon valeur par défaut locale. dataDir = dossier utilisateur pour secrets.
 */
data class AppConfig(
    val baseUrl: String,
    val dataDir: File,
    val enableHttpLogging: Boolean,
) {
    companion object {
        fun load(): AppConfig {
            val baseUrl = System.getenv("EJDR_API_URL") ?: "http://localhost:3000"
            val appData = System.getenv("APPDATA")
                ?: System.getProperty("user.home")
            val dataDir = File(appData, "E-JDR").apply { mkdirs() }
            val logging = System.getenv("EJDR_HTTP_LOG")?.toBoolean() ?: true
            return AppConfig(baseUrl = baseUrl, dataDir = dataDir, enableHttpLogging = logging)
        }
    }
}
