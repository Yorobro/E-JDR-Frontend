package eu.ejdr.presentation.shared.component.atomic

/**
 * Garde anti double-clic, pure et testable.
 *
 * Décide si un clic doit être accepté ou ignoré (rapproché d'un clic déjà accepté).
 * L'horodatage est INJECTÉ via [tryClick] : aucune horloge interne, donc testable sans
 * temps réel et multiplateforme. L'usage @Composable (fenêtre temporelle réelle) est câblé
 * dans AppButton.
 *
 * @param windowMs Fenêtre de garde en millisecondes (deux clics plus rapprochés que ça →
 * le second est ignoré).
 */
class ClickGuard(private val windowMs: Long = 400L) {
    private var lastAcceptedMs: Long? = null

    /** @return true si le clic est accepté (et arme la fenêtre), false s'il est ignoré. */
    fun tryClick(nowMs: Long): Boolean {
        val last = lastAcceptedMs
        if (last != null && nowMs - last < windowMs) return false
        lastAcceptedMs = nowMs
        return true
    }
}
