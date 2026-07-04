package eu.ejdr.application.features.realtime.abstraction

import kotlinx.coroutines.flow.Flow

/** Signal « tel périmètre a changé, recharge-le ». */
data class Invalidation(val resource: String, val scopeId: String)

/**
 * Bus applicatif d'invalidation : la couche realtime y publie les signaux reçus du serveur,
 * les ViewModels les observent pour recharger les écrans concernés via REST.
 */
interface InvalidationBus {
    /** Flux des invalidations (chaud : seuls les abonnés au moment de l'émission reçoivent). */
    val events: Flow<Invalidation>

    /** Publie une invalidation à destination des abonnés. */
    suspend fun emit(invalidation: Invalidation)
}
