package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import kotlinx.coroutines.flow.Flow

/**
 * Seam **bas niveau** d'une session temps réel ouverte.
 *
 * Isole la mécanique du transport (ouverture du socket, envoi/réception de trames) de
 * la **machine à états** de reconnexion portée par [KtorRealtimeConnection]. Ce
 * découpage rend la logique de reconnexion testable avec un transport simulé, sans
 * vrai serveur WebSocket.
 */
interface RealtimeTransport {

    /**
     * Ouvre une session et émet les messages entrants jusqu'à la fermeture du flux
     * (fin normale = serveur a fermé proprement ; exception = coupure à reconnecter).
     *
     * Avant l'ouverture, l'implémentation s'assure d'une session valide
     * (ré-authentification préalable), puisqu'une connexion longue durée n'est pas
     * couverte par l'intercepteur 401 du client HTTP.
     *
     * @return Un flux froid des messages entrants pour cette session.
     */
    fun open(): Flow<RealtimeMessage>

    /**
     * Envoie un message sur la session courante.
     *
     * @param message Message à transmettre.
     */
    suspend fun send(message: RealtimeMessage)
}
