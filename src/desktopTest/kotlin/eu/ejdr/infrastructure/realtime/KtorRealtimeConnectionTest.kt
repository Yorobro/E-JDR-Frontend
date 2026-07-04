package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.ConnectionState
import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Teste la **machine à états de reconnexion** de [KtorRealtimeConnection] avec un
 * transport simulé — sans vrai serveur WebSocket. C'est précisément la complexité que
 * l'on veut couverte avant qu'une feature temps réel n'en dépende.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KtorRealtimeConnectionTest {

    private val msg = RealtimeMessage(type = "ping", payload = "{}")

    /** Coupure de socket simulée (exception spécifique pour satisfaire detekt). */
    private class SocketDropped : RuntimeException("socket dropped")

    /** Transport dont chaque appel à [open] exécute le n-ième scénario fourni. */
    private class ScriptedTransport(
        private val scripts: List<suspend kotlinx.coroutines.flow.FlowCollector<RealtimeMessage>.() -> Unit>,
    ) : RealtimeTransport {
        val opens = AtomicInteger(0)
        override fun open(): Flow<RealtimeMessage> {
            val index = opens.getAndIncrement().coerceAtMost(scripts.lastIndex)
            return flow { scripts[index](this) }
        }
        override suspend fun send(message: RealtimeMessage) = Unit
        override suspend fun sendRaw(text: String) = Unit
    }

    private fun TestScope.connection(
        transport: RealtimeTransport,
        onReconnected: suspend () -> Unit = {},
    ) = KtorRealtimeConnection(
        scope = this,
        transport = transport,
        reconnectPolicy = ReconnectPolicy(baseDelayMs = 1, maxDelayMs = 1, jitterMs = 0, jitter = { 0.0 }),
        onReconnected = onReconnected,
    )

    @Test
    fun `transitions to Connected and forwards messages`() = runTest {
        // Session qui émet puis reste ouverte (awaitCancellation) => reste Connected.
        val transport = ScriptedTransport(listOf({ emit(msg); awaitCancellation() }))
        val conn = connection(transport)

        conn.connect()
        val received = conn.incoming.first()
        advanceUntilIdle()

        assertEquals(msg, received)
        assertEquals(ConnectionState.Connected, conn.state.value)
        conn.disconnect()
    }

    @Test
    fun `reconnects after a crash then invokes onReconnected`() = runTest {
        var reconnectedCalls = 0
        // 1re session : émet puis CRASHE ; 2e session : émet et reste ouverte.
        val transport = ScriptedTransport(
            listOf(
                { emit(msg); throw SocketDropped() },
                { emit(msg); awaitCancellation() },
            ),
        )
        val conn = connection(transport) { reconnectedCalls++ }

        conn.connect()
        advanceUntilIdle()

        assertTrue(transport.opens.get() >= 2, "should have reopened after the crash")
        assertEquals(1, reconnectedCalls, "onReconnected runs once after the successful reconnect")
        assertEquals(ConnectionState.Connected, conn.state.value)
        conn.disconnect()
    }

    @Test
    fun `stops cleanly when the server closes normally`() = runTest {
        // Une seule session qui se termine NORMALEMENT (pas d'exception) : pas de reconnexion.
        val transport = ScriptedTransport(listOf({ emit(msg) /* fin normale du flux */ }))
        val conn = connection(transport)

        conn.connect()
        advanceUntilIdle()

        assertEquals(1, transport.opens.get(), "a normal close must not trigger a reconnect")
        assertEquals(ConnectionState.Disconnected, conn.state.value)
    }

    @Test
    fun `disconnect stops the loop and reports Disconnected`() = runTest {
        // Session qui reste ouverte indéfiniment jusqu'à annulation.
        val transport = ScriptedTransport(
            listOf({ emit(msg); awaitCancellation() }),
        )
        val conn = connection(transport)

        conn.connect()
        conn.incoming.first()
        conn.disconnect()
        advanceUntilIdle()

        assertEquals(ConnectionState.Disconnected, conn.state.value)
    }
}
