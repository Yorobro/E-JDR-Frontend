package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.file.DesktopFileSaver
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.DpapiSecretProtector
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.PlaintextSecretProtector
import eu.ejdr.infrastructure.security.SecretProtector
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
    // DPAPI sous Windows (secret lié à l'utilisateur), repli en clair ailleurs (OS non ciblé).
    single<SecretProtector> {
        if (System.getProperty("os.name").orEmpty().startsWith("Windows")) DpapiSecretProtector()
        else PlaintextSecretProtector()
    }
    single { KeyStoreProvider(get<AppConfig>().dataDir, get<SecretProtector>()) }
    single { CookieCipher(get()) }
    single {
        SecureCookiesStorage(
            get<AppConfig>().dataDir,
            get(),
            get<AppConfig>().baseUrl,
            AcceptAllCookiesStorage(),
        )
    }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> { KtorClientFactory(get(), get<SecureCookiesStorage>()).create() }
    single<FileSaver> { DesktopFileSaver() }
}
