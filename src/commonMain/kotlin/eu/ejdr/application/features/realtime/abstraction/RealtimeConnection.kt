package eu.ejdr.application.features.realtime.abstraction

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * État observable d'une connexion temps réel.
 */
enum class ConnectionState {
    /** Aucune connexion active. */
    Disconnected,

    /** Connexion (ou re-authentification préalable) en cours. */
    Connecting,

    /** Connexion établie et opérationnelle. */
    Connected,

    /** Connexion perdue ; tentative de reconnexion (backoff) en cours. */
    Reconnecting,
}

/**
 * Enveloppe minimale d'un message temps réel.
 *
 * Volontairement **générique** à ce stade : un `type` discriminant et un `payload`
 * JSON brut. Le mapping vers des messages métier riches (campagne, jet de dé, chat)
 * viendra **avec** la feature temps réel concernée, quand le protocole serveur sera
 * connu — on ne le devine pas ici.
 *
 * @property type Discriminant applicatif du message (ex. `"dice.roll"`).
 * @property payload Charge utile JSON brute, à désérialiser par le consommateur.
 */
data class RealtimeMessage(
    val type: String,
    val payload: String,
)

/**
 * Port d'accès à une connexion temps réel (WebSocket), côté application.
 *
 * Abstrait une connexion **longue durée** : contrairement aux appels REST, elle n'est
 * pas couverte par l'intercepteur 401 du client HTTP. L'implémentation
 * d'infrastructure gère donc elle-même l'authentification avant connexion, la
 * reconnexion avec backoff et la re-souscription.
 *
 * Aucun écran ne consomme encore ce port : il est fourni prêt à l'emploi pour la
 * première feature temps réel.
 */
interface RealtimeConnection {

    /** État courant de la connexion, observable. */
    val state: StateFlow<ConnectionState>

    /** Flux des messages entrants (déjà dé-enveloppés). */
    val incoming: Flow<RealtimeMessage>

    /**
     * Ouvre la connexion (ré-authentification préalable incluse) et la maintient,
     * en se reconnectant automatiquement en cas de coupure. Suspend jusqu'à
     * l'établissement initial ou l'abandon.
     */
    suspend fun connect()

    /**
     * Envoie un message au serveur.
     *
     * @param message Message à transmettre.
     */
    suspend fun send(message: RealtimeMessage)

    /**
     * Envoie un message de contrôle **brut** (texte JSON déjà sérialisé) tel quel, sans
     * enveloppe. Utilisé pour les frames subscribe/unsubscribe dont le format ({type, channel})
     * diffère de l'enveloppe métier {type, payload}.
     */
    suspend fun sendRaw(text: String)

    /** Ferme la connexion et arrête toute tentative de reconnexion. */
    suspend fun disconnect()
}
