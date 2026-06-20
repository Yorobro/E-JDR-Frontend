package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
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
import eu.ejdr.infrastructure.settings.ActiveGroupFileRepository
import eu.ejdr.infrastructure.settings.ThemeFileRepository
import eu.ejdr.infrastructure.system.WindowsSystemLauncher
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module
import java.io.File

/**
 * Module Koin d'infrastructure **desktop**.
 *
 * Porte tout le socle transverse (config, sécurité DPAPI/JCEKS, HttpClient Ktor CIO) ainsi
 * que les **bindings platform-specific** des features dont l'implémentation dépend de la
 * plateforme : persistance du thème et du groupe actif (fichiers `%APPDATA%`), lancement
 * système (installateur Windows), sauvegarde de fichier (dialogue AWT) et l'état global
 * [ActiveGroupState]. Le pendant Android fournira ces mêmes bindings via ses propres impls.
 */
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

    // Bindings platform-specific des features (bifurqués depuis leurs modules communs)
    single<ThemeRepository> { ThemeFileRepository(File(get<AppConfig>().dataDir)) }
    single<SystemLauncherService> { WindowsSystemLauncher() }
    single<ActiveGroupRepository> { ActiveGroupFileRepository(File(get<AppConfig>().dataDir)) }
    single { ActiveGroupState(get(), get(), get()) }
}
