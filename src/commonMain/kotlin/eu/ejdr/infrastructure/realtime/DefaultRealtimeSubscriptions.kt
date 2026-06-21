package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implémentation par défaut : maintient l'ensemble des canaux voulus et envoie les frames
 * de contrôle via [RealtimeConnection.sendRaw]. Les envois sont lancés sur [scope] (les
 * appels publics sont non-suspendants pour rester simples côté ViewModel).
 *
 * @property connection Connexion temps réel (envoi des frames).
 * @property scope Portée portant les envois asynchrones.
 */
class DefaultRealtimeSubscriptions(
    private val connection: RealtimeConnection,
    private val scope: CoroutineScope,
) : RealtimeSubscriptions {

    private val mutex = Mutex()
    private val channels = mutableSetOf<String>()

    override fun subscribe(channel: String) {
        scope.launch {
            val added = mutex.withLock { channels.add(channel) }
            if (added) connection.sendRaw(frame("subscribe", channel))
        }
    }

    override fun unsubscribe(channel: String) {
        scope.launch {
            val removed = mutex.withLock { channels.remove(channel) }
            if (removed) connection.sendRaw(frame("unsubscribe", channel))
        }
    }

    override suspend fun resubscribeAll() {
        val snapshot = mutex.withLock { channels.toList() }
        for (channel in snapshot) {
            connection.sendRaw(frame("subscribe", channel))
        }
    }

    private fun frame(type: String, channel: String): String =
        """{"type":"$type","channel":"$channel"}"""
}
