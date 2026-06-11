package eu.ejdr.di

import org.koin.core.context.startKoin

/**
 * Composition root de l'application : démarre le conteneur Koin.
 *
 * Charge l'ensemble des modules ([infrastructureModule], [applicationModule] puis
 * [realtimeModule]) afin que toutes les dépendances soient câblées avant le lancement
 * de l'interface. À appeler une seule fois au démarrage.
 *
 * @return L'application Koin initialisée.
 */
fun initKoin() = startKoin {
    modules(infrastructureModule, applicationModule, realtimeModule)
}
