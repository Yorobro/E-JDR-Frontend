package eu.ejdr.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Composition root **Android** : démarre Koin avec le `Context` applicatif et charge le module
 * d'infrastructure Android ([infrastructureModule] = `InfrastructureModuleAndroid`) plus les
 * 8 modules feature communs (identiques au desktop). À appeler une fois dans [Application.onCreate].
 */
fun initKoinAndroid(app: Application) = startKoin {
    androidContext(app)
    modules(
        infrastructureModule,
        authModule,
        settingsModule,
        updateModule,
        campaignModule,
        sessionModule,
        characterSheetModule,
        referenceModule,
        friendGroupModule,
        realtimeModule,
    )
}
