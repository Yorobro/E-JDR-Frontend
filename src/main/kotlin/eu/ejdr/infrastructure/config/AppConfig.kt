package eu.ejdr.infrastructure.config

import java.io.File

/**
 * Config applicative.
 *
 * `baseUrl` choisit l'API ciblée. Résolution par ordre de priorité décroissant :
 * 1. variable d'environnement OS `EJDR_API_URL` (utile en CI / build packagé) ;
 * 2. clé `EJDR_API_URL` d'un fichier `.env` local (override de dev, non commité) ;
 * 3. valeur par défaut [PROD_API_URL] (prod), pour que l'app packagée fonctionne
 *    sans configuration.
 *
 * Concrètement : copier `.env.example` en `.env` et choisir l'URL voulue suffit
 * à basculer entre l'API de dev et celle de prod, sans toucher au code.
 *
 * `dataDir` = dossier utilisateur pour les secrets.
 */
data class AppConfig(
    val baseUrl: String,
    val dataDir: File,
    val enableHttpLogging: Boolean,
) {
    companion object {
        const val PROD_API_URL = "https://ejdr-backend.vyxs.fr"
        const val DEV_API_URL = "http://localhost:3000"

        fun load(): AppConfig = load(System.getenv(), EnvFile.load())

        /**
         * Variante testable : [osEnv] = variables d'environnement OS,
         * [dotenv] = contenu du fichier `.env`.
         */
        internal fun load(
            osEnv: Map<String, String>,
            dotenv: Map<String, String>,
        ): AppConfig {
            fun resolve(key: String): String? = osEnv[key] ?: dotenv[key]

            val baseUrl = resolve("EJDR_API_URL") ?: PROD_API_URL
            val appData = resolve("APPDATA") ?: System.getProperty("user.home")
            val dataDir = File(appData, "E-JDR").apply { mkdirs() }
            val logging = resolve("EJDR_HTTP_LOG")?.toBoolean() ?: false
            return AppConfig(baseUrl = baseUrl, dataDir = dataDir, enableHttpLogging = logging)
        }
    }
}
