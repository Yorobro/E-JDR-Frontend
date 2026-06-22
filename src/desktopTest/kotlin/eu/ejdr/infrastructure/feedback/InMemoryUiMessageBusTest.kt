package eu.ejdr.infrastructure.feedback

import eu.ejdr.application.shared.feedback.UiMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryUiMessageBusTest {
    @Test
    fun `un message emis est recu par les abonnes`() = runTest {
        val bus = InMemoryUiMessageBus()
        val received = async { bus.messages.first() }
        // laisser l'abonnement s'installer avant d'émettre (SharedFlow replay=0)
        yield()
        bus.emit(UiMessage.success("ok"))
        assertEquals("ok", received.await().text)
    }
}
