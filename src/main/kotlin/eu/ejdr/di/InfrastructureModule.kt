package eu.ejdr.di

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.http.auth.AuthHttpMapper
import eu.ejdr.infrastructure.http.auth.AuthHttpRepository
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module

val infrastructureModule = module {
    single { AppConfig.load() }
    single { KeyStoreProvider(get<AppConfig>().dataDir) }
    single { CookieCipher(get()) }
    single { SecureCookiesStorage(get<AppConfig>().dataDir, get(), AcceptAllCookiesStorage()) }
    single<HttpClient> { KtorClientFactory(get(), get<SecureCookiesStorage>()).create() }
    single { AuthHttpMapper() }
    single<AuthRepository> { AuthHttpRepository(get(), get(), get(), get<SecureCookiesStorage>()) }
}
