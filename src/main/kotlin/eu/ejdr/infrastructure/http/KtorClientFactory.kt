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

class KtorClientFactory(
    private val config: AppConfig,
    private val cookiesStorage: CookiesStorage,
) {
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
