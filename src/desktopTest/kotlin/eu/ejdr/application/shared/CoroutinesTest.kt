package eu.ejdr.application.shared

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Vérifie l'invariant essentiel de [runCatchingCancellable] : il encapsule les
 * throwables ordinaires mais **laisse passer** l'annulation de coroutine, afin de
 * préserver la concurrence structurée.
 */
class CoroutinesTest {

    @Test
    fun `returns success when block succeeds`() {
        val result = runCatchingCancellable { 42 }
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `captures ordinary throwable as failure`() {
        val boom = IllegalStateException("boom")
        val result = runCatchingCancellable { throw boom }
        assertTrue(result.isFailure)
        assertEquals(boom, result.exceptionOrNull())
    }

    @Test
    fun `rethrows CancellationException instead of capturing it`() {
        assertFailsWith<CancellationException> {
            runCatchingCancellable { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `does not swallow cancellation of the enclosing coroutine`() = runTest {
        val started = CompletableDeferred<Unit>()
        // Une coroutine qui, une fois annulée, vérifie que l'annulation se propage
        // bien au lieu d'être convertie en échec ordinaire par le helper.
        val job = async {
            started.complete(Unit)
            runCatchingCancellable {
                // Suspend "infiniment" : sera interrompu par l'annulation.
                CompletableDeferred<Int>().await()
            }
        }
        started.await()
        job.cancel()
        assertFailsWith<CancellationException> { job.await() }
    }
}
