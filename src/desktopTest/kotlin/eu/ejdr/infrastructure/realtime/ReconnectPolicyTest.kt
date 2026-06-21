package eu.ejdr.infrastructure.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReconnectPolicyTest {

    @Test
    fun `delay grows exponentially from the base`() {
        // Jitter neutralisé (0.0) pour un calcul déterministe.
        val policy = ReconnectPolicy(baseDelayMs = 500, maxDelayMs = 30_000, jitterMs = 1_000, jitter = { 0.0 })
        assertEquals(500, policy.delayForAttempt(0))
        assertEquals(1_000, policy.delayForAttempt(1))
        assertEquals(2_000, policy.delayForAttempt(2))
        assertEquals(4_000, policy.delayForAttempt(3))
    }

    @Test
    fun `delay is capped at maxDelayMs`() {
        val policy = ReconnectPolicy(baseDelayMs = 500, maxDelayMs = 30_000, jitterMs = 0, jitter = { 0.0 })
        // 500 * 2^10 = 512_000 -> plafonné à 30_000.
        assertEquals(30_000, policy.delayForAttempt(10))
    }

    @Test
    fun `jitter is added on top of the capped delay`() {
        val policy = ReconnectPolicy(baseDelayMs = 500, maxDelayMs = 30_000, jitterMs = 1_000, jitter = { 1.0 })
        // base 500 + jitter (1.0 * 1000) = 1500.
        assertEquals(1_500, policy.delayForAttempt(0))
    }

    @Test
    fun `jitter stays within its declared amplitude`() {
        val policy = ReconnectPolicy(baseDelayMs = 100, maxDelayMs = 30_000, jitterMs = 1_000, jitter = { 0.5 })
        val delay = policy.delayForAttempt(0)
        assertTrue(delay in 100..1_100, "delay $delay should be base + at most jitterMs")
    }
}
