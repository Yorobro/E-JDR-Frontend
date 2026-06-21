package eu.ejdr.application.features.realtime

import eu.ejdr.application.features.realtime.abstraction.ConnectionState
import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

class RealtimeCoordinatorTest {
    private class FakeConnection : RealtimeConnection {
        val inbound = MutableSharedFlow<RealtimeMessage>(extraBufferCapacity = 8)
        override val state: StateFlow<ConnectionState> =
            MutableStateFlow(ConnectionState.Connected)
        override val incoming = inbound
        var connected = false
        override suspend fun connect() {
            connected = true
        }
        override suspend fun send(message: RealtimeMessage) = Unit
        override suspend fun sendRaw(text: String) = Unit
        override suspend fun disconnect() = Unit
    }

    @Test
    fun `traduit un message invalidate en Invalidation sur le bus`() = runTest {
        val connection = FakeConnection()
        val bus = InMemoryInvalidationBus()
        val coordinator = RealtimeCoordinator(connection, bus, this)
        coordinator.start()
        yield()

        val received = mutableListOf<Invalidation>()
        val job = launch { received.add(bus.events.first()) }
        yield()
        connection.inbound.emit(
            RealtimeMessage(
                type = "invalidate",
                payload =
                    """{"type":"invalidate","channel":"user:u1","resource":"character-sheets","scopeId":"u1"}""",
            ),
        )
        job.join()

        assertEquals(listOf(Invalidation("character-sheets", "u1")), received)
        assertTrue(connection.connected)
        coordinator.stop()
    }
}
