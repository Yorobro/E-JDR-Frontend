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
 * **Caveat — pas de compteur de références :** L'ensemble [channels] n'est pas à compteur
 * de références. Si le même canal était abonné par deux appelants simultanément (p.ex. dans
 * une future UI multi-pane affichant la même fiche deux fois), le premier [unsubscribe]
 * le retirerait de l'ensemble et arrêterait les mises à jour pour les autres. La feature
 * actuelle (écran de détail unique) ne fait jamais cela, donc c'est sûr aujourd'hui.
 * Quand ce sera nécessaire, la correction sera de faire `Map<String, Int>` (ne dépublier
 * que quand le compteur atteint 0).
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
