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
 * **Compteur de références :** un même canal peut être voulu par plusieurs écrans à la fois
 * (typiquement `group:{id}`, suivi par la liste des campagnes, le détail du groupe **et** le
 * salon d'attente d'une session). [channels] compte donc les abonnés : le frame `subscribe`
 * n'est envoyé qu'au **premier**, et `unsubscribe` qu'au **dernier** parti. Sans ce compteur,
 * la fermeture d'un écran coupait le flux des autres (un joueur restait bloqué au salon,
 * jamais basculé vers l'écran de jeu).
 *
 * @property connection Connexion temps réel (envoi des frames).
 * @property scope Portée portant les envois asynchrones.
 */
class DefaultRealtimeSubscriptions(
    private val connection: RealtimeConnection,
    private val scope: CoroutineScope,
) : RealtimeSubscriptions {

    private val mutex = Mutex()

    /** Canaux voulus → nombre d'abonnés en cours (jamais d'entrée à 0). */
    private val channels = mutableMapOf<String, Int>()

    override fun subscribe(channel: String) {
        scope.launch {
            val isFirst = mutex.withLock {
                val count = (channels[channel] ?: 0) + 1
                channels[channel] = count
                count == 1
            }
            if (isFirst) connection.sendRaw(frame("subscribe", channel))
        }
    }

    override fun unsubscribe(channel: String) {
        scope.launch {
            val isLast = mutex.withLock {
                val count = channels[channel] ?: return@withLock false
                if (count > 1) {
                    channels[channel] = count - 1
                    false
                } else {
                    channels.remove(channel)
                    true
                }
            }
            if (isLast) connection.sendRaw(frame("unsubscribe", channel))
        }
    }

    override suspend fun resubscribeAll() {
        val snapshot = mutex.withLock { channels.keys.toList() }
        for (channel in snapshot) {
            connection.sendRaw(frame("subscribe", channel))
        }
    }

    private fun frame(type: String, channel: String): String =
        """{"type":"$type","channel":"$channel"}"""
}
