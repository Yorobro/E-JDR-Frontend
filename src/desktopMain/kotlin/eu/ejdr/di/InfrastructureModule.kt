package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.config.load
import eu.ejdr.infrastructure.config.provideDataDir
import eu.ejdr.infrastructure.file.DesktopFileSaver
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.DpapiSecretProtector
import eu.ejdr.infrastructure.security.FileSessionStorage
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.PlaintextSecretProtector
import eu.ejdr.infrastructure.security.SecretProtector
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module
import java.io.File

val infrastructureModule = module {
    single { AppConfig.load() }
    single { provideDataDir() }
    single<SecretProtector> {
        if (System.getProperty("os.name").orEmpty().startsWith("Windows")) DpapiSecretProtector()
        else PlaintextSecretProtector()
    }
    single { KeyStoreProvider(get<File>(), get<SecretProtector>()) }
    single { CookieCipher(get()) }
    single { FileSessionStorage(get<File>(), get<CookieCipher>()) }
    single {
        SecureCookiesStorage(
            sessionStorage = get<FileSessionStorage>(),
            backendUrl = get<AppConfig>().baseUrl,
            delegate = AcceptAllCookiesStorage(),
        )
    }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> {
        KtorClientFactory(
            config = get(),
            cookiesStorage = get<SecureCookiesStorage>(),
            sessionPersistence = get<SessionPersistence>(),
            engineFactory = CIO,
        ).create()
    }
    single<FileSaver> { DesktopFileSaver() }
}
