package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.shared.Result
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.realtime.KtorRealtimeConnection
import eu.ejdr.infrastructure.realtime.KtorWebSocketTransport
import eu.ejdr.infrastructure.realtime.RealtimeTransport
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Module Koin de la couche temps réel (WebSocket).
 *
 * Amorce le découpage des modules **par feature** (au-delà des deux modules globaux
 * `infrastructureModule`/`applicationModule`). Fournit :
 * - une portée de coroutine dédiée (non liée à un écran) pour la connexion ;
 * - le [RealtimeTransport] Ktor (auth-on-connect via [AuthRepository.refresh]) ;
 * - le [RealtimeConnection] (machine à états de reconnexion).
 *
 * Aucun écran ne le consomme encore : il est prêt pour la première feature temps réel.
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
        KtorRealtimeConnection(scope = get(), transport = get())
    }
}
