package eu.ejdr.di

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.update.abstraction.repository.UpdateRepository
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.http.auth.AuthHttpMapper
import eu.ejdr.infrastructure.http.auth.AuthHttpRepository
import eu.ejdr.infrastructure.http.update.UpdateHttpRepository
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import eu.ejdr.infrastructure.settings.ThemeFileRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module

/**
 * Module Koin de la couche infrastructure.
 *
 * Il fournit les briques techniques (configuration, sécurité, accès réseau) et
 * câble les implémentations infra aux ports déclarés par la couche application.
 * En particulier, le port [AuthRepository] est lié à son implémentation HTTP
 * [AuthHttpRepository] : la présentation ne dépend ainsi que de l'interface.
 *
 * Composants exposés : [AppConfig], [KeyStoreProvider], [CookieCipher],
 * [SecureCookiesStorage], le [HttpClient] (via [KtorClientFactory]),
 * [AuthHttpMapper] et [AuthRepository].
 */
val infrastructureModule = module {
    single { AppConfig.load() }
    single { KeyStoreProvider(get<AppConfig>().dataDir) }
    single { CookieCipher(get()) }
    single { SecureCookiesStorage(get<AppConfig>().dataDir, get(), AcceptAllCookiesStorage()) }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> { KtorClientFactory(get(), get<SecureCookiesStorage>()).create() }
    single { AuthHttpMapper }
    single<AuthRepository> { AuthHttpRepository(get(), get(), get(), get<SessionPersistence>()) }
    single<UpdateRepository> { UpdateHttpRepository(get()) }
    single<ThemeRepository> { ThemeFileRepository(get<AppConfig>().dataDir) }
}
