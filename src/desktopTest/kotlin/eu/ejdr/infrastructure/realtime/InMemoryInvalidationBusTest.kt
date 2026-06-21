package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.Invalidation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

class InMemoryInvalidationBusTest {
    @Test
    fun `emit délivre l'invalidation aux abonnés`() = runTest {
        val bus = InMemoryInvalidationBus()
        val received = mutableListOf<Invalidation>()
        val job = launch { received.add(bus.events.first()) }
        yield()
        bus.emit(Invalidation(resource = "character-sheets", scopeId = "u1"))
        job.join()
        assertEquals(listOf(Invalidation("character-sheets", "u1")), received)
    }
}
