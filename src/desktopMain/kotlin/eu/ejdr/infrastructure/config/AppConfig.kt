package eu.ejdr.infrastructure.config

import eu.ejdr.BuildConfig
import java.io.File

/**
 * Config applicative.
 *
 * `baseUrl` et `enableHttpLogging` proviennent de [BuildConfig], donc résolus AU BUILD
 * (depuis `config.defaults.properties` + surcharge `config.local.properties`) et non plus
 * de l'environnement système : reproductible et portable (desktop / Android / iOS).
 *
 * `dataDir` reste un lookup plateforme (dossier utilisateur pour les secrets) — c'est de la
 * plateforme, pas de la config d'environnement. Lors du passage en Compose Multiplatform, seul
 * ce calcul deviendra un `expect/actual` (APPDATA sur Windows, filesDir sur Android, etc.) ;
 * `baseUrl`/`enableHttpLogging` migreront tels quels en commonMain.
 */
data class AppConfig(
    val baseUrl: String,
    val dataDir: File,
    val enableHttpLogging: Boolean,
) {
    companion object {
        fun load(): AppConfig {
            val appData = System.getenv("APPDATA")
                ?: System.getProperty("user.home")
            val dataDir = File(appData, "E-JDR").apply { mkdirs() }
            return AppConfig(
                baseUrl = BuildConfig.API_URL,
                dataDir = dataDir,
                enableHttpLogging = BuildConfig.HTTP_LOGGING,
            )
        }
    }
}
