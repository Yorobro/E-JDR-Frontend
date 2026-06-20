package eu.ejdr.infrastructure.config

import eu.ejdr.BuildConfig

/**
 * Config applicative.
 *
 * `baseUrl` et `enableHttpLogging` proviennent de [BuildConfig], donc résolus AU BUILD
 * (depuis `config.defaults.properties` + surcharge `config.local.properties`) et non plus
 * de l'environnement système : reproductible et portable (desktop / Android / iOS).
 *
 * `dataDir` est le chemin (String) du dossier de données de l'utilisateur — sa résolution
 * est plateforme-dépendante et délèguée à `AppConfig.load()` dans chaque sourceSet
 * (APPDATA sur Windows, filesDir sur Android, etc.).
 */
data class AppConfig(
    val baseUrl: String,
    val dataDir: String,
    val enableHttpLogging: Boolean,
) {
    companion object
}
