package eu.ejdr.di

import org.koin.core.context.startKoin

/**
 * Composition root de l'application : démarre le conteneur Koin.
 *
 * Charge le socle transverse ([infrastructureModule]) puis un module **par feature**
 * ([authModule], [settingsModule], [updateModule], [realtimeModule], [campaignModule],
 * [sessionModule], [characterSheetModule], [referenceModule]). Ajouter une feature = ajouter son
 * module ici, sans toucher aux autres. À appeler une seule fois au démarrage.
 *
 * @return L'application Koin initialisée.
 */
fun initKoin() = startKoin {
    modules(
        infrastructureModule,
        authModule,
        settingsModule,
        updateModule,
        realtimeModule,
        campaignModule,
        sessionModule,
        characterSheetModule,
        referenceModule,
        friendGroupModule,
    )
}
