package eu.ejdr.di

import android.content.Context
import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.config.loadAppConfig
import eu.ejdr.infrastructure.file.AndroidFileSaver
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.AndroidSessionStorage
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import eu.ejdr.infrastructure.security.SessionStorage
import eu.ejdr.infrastructure.settings.AndroidActiveGroupRepository
import eu.ejdr.infrastructure.settings.AndroidThemeRepository
import eu.ejdr.infrastructure.system.AndroidUpdateLauncher
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Module Koin d'infrastructure **Android**.
 *
 * Pendant mobile d'`InfrastructureModuleDesktop` : même graphe logique, implémentations Android.
 * Sécurité via Android Keystore + EncryptedSharedPreferences ([AndroidSessionStorage]), moteur
 * HTTP OkHttp, persistance thème/groupe en SharedPreferences, partage de fichier via share sheet,
 * mise à jour déléguée au Play Store. La config commune (`loadAppConfig()`) ne porte que
 * `baseUrl` + `httpLogging` ; le dossier de données Android est `context.filesDir`.
 */
val infrastructureModule = module {
    single { loadAppConfig() }
    single<SessionStorage> { AndroidSessionStorage(androidContext()) }
    single {
        SecureCookiesStorage(
            sessionStorage = get<SessionStorage>(),
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
            engineFactory = OkHttp,
        ).create()
    }

    single<ThemeRepository> { AndroidThemeRepository(androidContext()) }
    single<ActiveGroupRepository> { AndroidActiveGroupRepository(androidContext()) }
    single<FileSaver> { AndroidFileSaver(androidContext()) }
    single<SystemLauncherService> { AndroidUpdateLauncher(androidContext()) }
    single { ActiveGroupState(get(), get(), get()) }
}
