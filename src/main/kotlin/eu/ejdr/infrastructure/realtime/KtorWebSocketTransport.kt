package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import eu.ejdr.infrastructure.realtime.dto.RealtimeEnvelopeDto
import eu.ejdr.infrastructure.realtime.dto.toDomain
import eu.ejdr.infrastructure.realtime.dto.toDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * Adaptateur Ktor du [RealtimeTransport] : ouvre une session WebSocket réelle.
 *
 * **Auth-on-connect** : avant d'ouvrir le socket, [ensureSession] est invoqué (refresh
 * proactif du token). C'est nécessaire car une connexion WebSocket longue durée n'est
 * pas couverte par l'intercepteur 401 du client HTTP REST ; se connecter avec un token
 * expiré échouerait sans recours. Les cookies de session sont envoyés automatiquement
 * par le [client] partagé (même `SecureCookiesStorage`).
 *
 * La machine à états de reconnexion vit dans [KtorRealtimeConnection] ; ce transport ne
 * gère qu'**une** session à la fois.
 *
 * @property client Client HTTP partagé (plugin WebSockets installé, cookies de session).
 * @property url URL du endpoint WebSocket (ex. `wss://…/ws`).
 * @property ensureSession Refresh proactif de session avant connexion ; `false` annule l'ouverture.
 */
class KtorWebSocketTransport(
    private val client: HttpClient,
    private val url: String,
    private val ensureSession: suspend () -> Boolean,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : RealtimeTransport {

    @Volatile
    private var session: WebSocketSession? = null

    override fun open(): Flow<RealtimeMessage> = flow {
        check(ensureSession()) { "Refresh de session échoué avant connexion WebSocket" }
        val ws = client.webSocketSession(urlString = url)
        session = ws
        try {
            for (frame in ws.incoming) {
                if (frame is Frame.Text) {
                    emit(decode(frame.readText()))
                }
            }
        } finally {
            session = null
        }
    }

    override suspend fun send(message: RealtimeMessage) {
        val ws = session ?: error("Aucune session WebSocket ouverte")
        ws.send(json.encodeToString(RealtimeEnvelopeDto.serializer(), message.toDto()))
    }

    private fun decode(text: String): RealtimeMessage =
        json.decodeFromString(RealtimeEnvelopeDto.serializer(), text).toDomain()
}
