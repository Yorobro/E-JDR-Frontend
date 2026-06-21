package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus d'invalidation en mémoire (SharedFlow). `replay = 0` : une invalidation n'a de sens que
 * pour les écrans actuellement ouverts ; `extraBufferCapacity` évite de bloquer l'émetteur.
 */
class InMemoryInvalidationBus : InvalidationBus {
    private val mutableEvents =
        MutableSharedFlow<Invalidation>(replay = 0, extraBufferCapacity = 64)
    override val events: Flow<Invalidation> = mutableEvents.asSharedFlow()

    override suspend fun emit(invalidation: Invalidation) {
        mutableEvents.emit(invalidation)
    }
}
