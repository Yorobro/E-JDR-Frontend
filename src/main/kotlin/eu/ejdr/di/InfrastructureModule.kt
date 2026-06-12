package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module

/**
 * Module Koin **socle** de l'infrastructure : briques techniques transverses partagées par
 * toutes les features (configuration, sécurité/coffre, client HTTP). Les bindings propres à
 * une feature (auth, settings, update, realtime) vivent dans leur module dédié.
 */
val infrastructureModule = module {
    single { AppConfig.load() }
    single { KeyStoreProvider(get<AppConfig>().dataDir) }
    single { CookieCipher(get()) }
    single { SecureCookiesStorage(get<AppConfig>().dataDir, get(), AcceptAllCookiesStorage()) }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> { KtorClientFactory(get(), get<SecureCookiesStorage>()).create() }
}
