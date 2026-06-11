package eu.ejdr.infrastructure.realtime

import kotlin.math.min
import kotlin.math.pow

/**
 * Politique de reconnexion à backoff exponentiel **plafonné**, avec jitter.
 *
 * Le délai avant la n-ième tentative croît exponentiellement (`base * 2^n`) jusqu'à un
 * plafond [maxDelayMs], puis un jitter aléatoire est ajouté pour éviter que plusieurs
 * clients ne se reconnectent en rafale synchronisée (« thundering herd »).
 *
 * Le jitter est **injecté** ([jitter], défaut `Math.random()`) afin de rendre la
 * politique déterministe et donc testable.
 *
 * @property baseDelayMs Délai de base (1re tentative), en millisecondes.
 * @property maxDelayMs Plafond du délai exponentiel, en millisecondes.
 * @property jitterMs Amplitude maximale du jitter ajouté, en millisecondes.
 * @property jitter Source du jitter dans `[0,1)` (injectable pour les tests).
 */
class ReconnectPolicy(
    private val baseDelayMs: Long = 500,
    private val maxDelayMs: Long = 30_000,
    private val jitterMs: Long = 1_000,
    private val jitter: () -> Double = { Math.random() },
) {
    /**
     * Délai à attendre avant la tentative numéro [attempt] (0 = première).
     *
     * @param attempt Index de la tentative, à partir de 0.
     * @return Le délai en millisecondes (backoff plafonné + jitter).
     */
    fun delayForAttempt(attempt: Int): Long {
        val exponential = baseDelayMs.toDouble() * 2.0.pow(attempt)
        val capped = min(exponential, maxDelayMs.toDouble()).toLong()
        val addedJitter = (jitter() * jitterMs).toLong()
        return capped + addedJitter
    }
}
