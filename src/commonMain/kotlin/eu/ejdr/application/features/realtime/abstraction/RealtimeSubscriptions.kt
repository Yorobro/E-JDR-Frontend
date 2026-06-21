package eu.ejdr.application.features.realtime.abstraction

/**
 * Registre central des abonnements temps réel actifs. Le ViewModel déclare les canaux
 * voulus ; le service envoie les frames subscribe/unsubscribe et réémet tous les abonnements
 * après une reconnexion (le serveur perd les abonnements à la coupure du socket).
 */
interface RealtimeSubscriptions {
    /** Demande l'abonnement à un canal (ex. "sheet:X"). Idempotent. */
    fun subscribe(channel: String)

    /** Annule l'abonnement à un canal. Idempotent. */
    fun unsubscribe(channel: String)

    /** Réémet un `subscribe` pour tous les canaux encore voulus (après reconnexion). */
    suspend fun resubscribeAll()
}
