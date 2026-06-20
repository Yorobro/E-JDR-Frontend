package eu.ejdr.infrastructure.config

import eu.ejdr.BuildConfig

/**
 * Config applicative **commune** à toutes les plateformes.
 *
 * `baseUrl` et `enableHttpLogging` proviennent de [BuildConfig], donc résolus AU BUILD
 * (depuis `config.defaults.properties` + surcharge `config.local.properties`) et non plus
 * de l'environnement système : reproductible et portable (desktop / Android / iOS).
 *
 * Le dossier de données utilisateur (ex-`dataDir`) est **plateforme-dépendant** (APPDATA sur
 * Windows, `context.filesDir` sur Android) : il ne fait plus partie de cette config commune et
 * est fourni séparément par chaque sourceSet (`provideDataDir()`).
 */
data class AppConfig(
    val baseUrl: String,
    val enableHttpLogging: Boolean,
)

/**
 * Construit la config commune depuis [BuildConfig]. Aucune I/O ni accès système : utilisable
 * tel quel sur toutes les plateformes.
 */
fun loadAppConfig(): AppConfig = AppConfig(
    baseUrl = BuildConfig.API_URL,
    enableHttpLogging = BuildConfig.HTTP_LOGGING,
)
