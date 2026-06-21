package eu.ejdr.application.features.realtime

import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.infrastructure.realtime.dto.InvalidationDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Pilote la couche temps réel côté application : ouvre la connexion et traduit les messages
 * entrants en invalidations publiées sur le [InvalidationBus]. Démarré après authentification,
 * arrêté à la déconnexion.
 *
 * @property connection Connexion temps réel (WebSocket) à piloter.
 * @property bus Bus sur lequel republier les invalidations reçues.
 * @property scope Portée de coroutine (non liée à un écran) qui porte la collecte/connexion.
 * @property json Désérialiseur tolérant des frames d'invalidation.
 */
class RealtimeCoordinator(
    private val connection: RealtimeConnection,
    private val bus: InvalidationBus,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private var collectJob: Job? = null
    private var connectJob: Job? = null

    /** Lance la connexion et la collecte des messages entrants. Idempotent. */
    fun start() {
        if (collectJob != null) return
        collectJob =
            scope.launch {
                connection.incoming.collect { message ->
                    if (message.type != "invalidate") return@collect
                    val dto =
                        runCatching {
                            json.decodeFromString(InvalidationDto.serializer(), message.payload)
                        }.getOrNull() ?: return@collect
                    bus.emit(Invalidation(resource = dto.resource, scopeId = dto.scopeId))
                }
            }
        connectJob = scope.launch { connection.connect() }
    }

    /** Arrête la collecte et la connexion. Idempotent. */
    fun stop() {
        collectJob?.cancel()
        collectJob = null
        connectJob?.cancel()
        connectJob = null
    }
}
