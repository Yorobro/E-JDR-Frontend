package eu.ejdr.infrastructure.http

import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

private val RefreshRetryKey = AttributeKey<Unit>("RefreshRetry")

/**
 * Fabrique le [HttpClient] Ktor partagé par la couche infrastructure.
 *
 * Centralise la configuration transverse du client :
 * - [HttpCookies] branché sur le [SecureCookiesStorage] fourni ;
 * - [ContentNegotiation] en JSON tolérant (clés inconnues ignorées, mode lenient) ;
 * - [Logging] HTTP optionnel, activé selon [AppConfig.enableHttpLogging] ;
 * - Intercepteur 401 : sur toute route hors `/auth/`, tente un rafraîchissement silencieux
 *   de session puis rejoue la requête originale. Si le refresh échoue, la session persistée
 *   est effacée et le 401 est retourné tel quel à l'appelant.
 *
 * `expectSuccess = false` laisse l'appelant inspecter lui-même les statuts d'échec.
 */
class KtorClientFactory(
    private val config: AppConfig,
    private val cookiesStorage: SecureCookiesStorage,
) {
    fun create(): HttpClient {
        val client = HttpClient(CIO) {
            expectSuccess = false
            install(HttpCookies) { storage = cookiesStorage }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            if (config.enableHttpLogging) {
                install(Logging) { level = LogLevel.INFO }
            }
        }

        client.plugin(HttpSend).intercept { request ->
            val call = execute(request)
            val isRetry = request.attributes.contains(RefreshRetryKey)
            val requestPath = request.url.build().encodedPath
            val isAuthRoute = requestPath.startsWith("/auth/")

            if (call.response.status != HttpStatusCode.Unauthorized || isRetry || isAuthRoute) {
                return@intercept call
            }

            // Tentative de rafraîchissement silencieux : un seul essai, pas de récursion.
            val refreshCall = execute(
                HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                    url { takeFrom("${config.baseUrl}/auth/refresh") }
                    attributes.put(RefreshRetryKey, Unit)
                },
            )

            if (!refreshCall.response.status.isSuccess()) {
                // Session expirée : on efface localement et on retourne le 401 original.
                cookiesStorage.clearPersisted()
                return@intercept call
            }

            // Nouveaux cookies posés par HttpCookies — rejoue la requête originale.
            request.attributes.put(RefreshRetryKey, Unit)
            execute(request)
        }

        return client
    }
}
