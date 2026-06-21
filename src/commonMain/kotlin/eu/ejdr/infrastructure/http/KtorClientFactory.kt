package eu.ejdr.infrastructure.http

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.infrastructure.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
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

class KtorClientFactory(
    private val config: AppConfig,
    private val cookiesStorage: CookiesStorage,
    private val sessionPersistence: SessionPersistence,
    private val engineFactory: HttpClientEngineFactory<*>,
) {
    fun create(): HttpClient {
        val client = HttpClient(engineFactory) {
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

            val refreshCall = execute(
                HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                    url { takeFrom("${config.baseUrl}/auth/refresh") }
                    attributes.put(RefreshRetryKey, Unit)
                },
            )

            if (!refreshCall.response.status.isSuccess()) {
                val refreshStatus = refreshCall.response.status
                if (refreshStatus == HttpStatusCode.Unauthorized ||
                    refreshStatus == HttpStatusCode.Forbidden
                ) {
                    sessionPersistence.clearPersisted()
                }
                return@intercept call
            }

            request.attributes.put(RefreshRetryKey, Unit)
            execute(request)
        }

        return client
    }
}
