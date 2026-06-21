package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.realtime.RealtimeCoordinator
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.shared.Result
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.realtime.DefaultRealtimeSubscriptions
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import eu.ejdr.infrastructure.realtime.KtorRealtimeConnection
import eu.ejdr.infrastructure.realtime.KtorWebSocketTransport
import eu.ejdr.infrastructure.realtime.RealtimeTransport
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Module Koin de la couche temps réel (WebSocket), **commun** aux plateformes.
 *
 * Découpage par feature : fournit une portée de coroutine dédiée (non liée à un écran), le
 * [RealtimeTransport] Ktor (auth-on-connect via [AuthRepository.refresh]), la
 * [RealtimeConnection] (machine à états de reconnexion), le [InvalidationBus] et le
 * [RealtimeCoordinator] qui traduit les messages entrants en invalidations.
 *
 * L'URL WebSocket dérive du `baseUrl` HTTP (`http`→`ws`, `https`→`wss`) + `/ws`. Le
 * `HttpClient` (avec plugin WebSockets et cookies de session) est fourni par le module
 * d'infrastructure de chaque plateforme.
 */
val realtimeModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob()) }

    single<RealtimeTransport> {
        val config = get<AppConfig>()
        val authRepository = get<AuthRepository>()
        KtorWebSocketTransport(
            client = get<HttpClient>(),
            url = config.baseUrl.replaceFirst("http", "ws").trimEnd('/') + "/ws",
            // Auth-on-connect : refresh proactif ; succès => connexion autorisée.
            ensureSession = { authRepository.refresh() is Result.Success },
        )
    }

    single<RealtimeConnection> {
        val subsLazy = lazy { get<RealtimeSubscriptions>() }
        KtorRealtimeConnection(
            scope = get(),
            transport = get(),
            onReconnected = { subsLazy.value.resubscribeAll() },
        )
    }

    // Bus d'invalidation : la couche realtime y publie, les ViewModels l'observent.
    single<InvalidationBus> { InMemoryInvalidationBus() }

    // Registre central des abonnements : envoie subscribe/unsubscribe et réémet après reconnexion.
    single<RealtimeSubscriptions> {
        DefaultRealtimeSubscriptions(connection = get(), scope = get())
    }

    // Coordinateur : connecte le WS et traduit les messages entrants en invalidations.
    // Démarré après authentification (cf. RootState), arrêté à la déconnexion.
    single { RealtimeCoordinator(connection = get(), bus = get(), scope = get()) }
}
