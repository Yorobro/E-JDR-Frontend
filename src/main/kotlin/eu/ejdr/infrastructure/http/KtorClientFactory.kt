package eu.ejdr.infrastructure.http

import eu.ejdr.infrastructure.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Fabrique le [HttpClient] Ktor partagé par la couche infrastructure.
 *
 * Centralise la configuration transverse du client :
 * - [HttpCookies] branché sur le [CookiesStorage] fourni, indispensable au
 *   modèle d'authentification par cookies (le serveur pose et lit les jetons) ;
 * - [ContentNegotiation] en JSON tolérant (clés inconnues ignorées, mode lenient)
 *   pour rester robuste face aux évolutions du contrat d'API ;
 * - [Logging] HTTP optionnel, activé selon [AppConfig.enableHttpLogging].
 *
 * `expectSuccess = false` laisse l'appelant inspecter lui-même les statuts
 * d'échec plutôt que de lever une exception.
 *
 * @property config Configuration applicative (logging notamment).
 * @property cookiesStorage Stockage des cookies à utiliser pour les sessions.
 */
class KtorClientFactory(
    private val config: AppConfig,
    private val cookiesStorage: CookiesStorage,
) {
    /**
     * Construit une instance configurée du client HTTP.
     *
     * @return Le [HttpClient] prêt à l'emploi.
     */
    fun create(): HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpCookies) { storage = cookiesStorage }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        if (config.enableHttpLogging) {
            install(Logging) { level = LogLevel.INFO }
        }
    }
}
