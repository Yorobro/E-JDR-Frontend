package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.ConnectionState
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRealtimeSubscriptionsTest {

    private class RecordingConnection : RealtimeConnection {
        val sent = mutableListOf<String>()
        override val state: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)
        override val incoming = MutableSharedFlow<RealtimeMessage>()
        override suspend fun connect() = Unit
        override suspend fun send(message: RealtimeMessage) = Unit
        override suspend fun sendRaw(text: String) { sent.add(text) }
        override suspend fun disconnect() = Unit
    }

    @Test
    fun `subscribe envoie un frame subscribe à plat`() = runTest {
        val conn = RecordingConnection()
        val subs = DefaultRealtimeSubscriptions(conn, this)
        subs.subscribe("sheet:s-1")
        advanceUntilIdle()
        assertEquals(listOf("""{"type":"subscribe","channel":"sheet:s-1"}"""), conn.sent)
    }

    @Test
    fun `unsubscribe envoie un frame unsubscribe et retire du set`() = runTest {
        val conn = RecordingConnection()
        val subs = DefaultRealtimeSubscriptions(conn, this)
        subs.subscribe("sheet:s-1")
        subs.unsubscribe("sheet:s-1")
        advanceUntilIdle()
        assertEquals(
            listOf(
                """{"type":"subscribe","channel":"sheet:s-1"}""",
                """{"type":"unsubscribe","channel":"sheet:s-1"}""",
            ),
            conn.sent,
        )
        conn.sent.clear()
        subs.resubscribeAll()
        advanceUntilIdle()
        assertTrue(conn.sent.isEmpty(), "un canal désabonné ne doit pas être réémis")
    }

    @Test
    fun `resubscribeAll réémet tous les canaux encore voulus`() = runTest {
        val conn = RecordingConnection()
        val subs = DefaultRealtimeSubscriptions(conn, this)
        subs.subscribe("sheet:a")
        subs.subscribe("sheet:b")
        advanceUntilIdle()
        conn.sent.clear()
        subs.resubscribeAll()
        advanceUntilIdle()
        assertEquals(
            setOf(
                """{"type":"subscribe","channel":"sheet:a"}""",
                """{"type":"subscribe","channel":"sheet:b"}""",
            ),
            conn.sent.toSet(),
        )
    }
}
