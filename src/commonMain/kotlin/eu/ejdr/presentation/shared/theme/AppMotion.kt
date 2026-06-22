package eu.ejdr.presentation.shared.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Tokens de mouvement du design system — **source unique des durées et courbes**.
 *
 * Comme [AppPalette] pour les couleurs : aucune durée/courbe d'animation n'est définie
 * ailleurs. Lu via [AppTheme.motion]. Pour ralentir/accélérer toute l'app, modifier ici.
 *
 * Personnalité : sobre & pro — rapide, ease-out doux, discret mais ressenti.
 *
 * @property enabled Quand `false`, [effectiveDuration] renvoie 0 → transitions instantanées
 * (réduction de mouvement / accessibilité), sans casser l'UI.
 */
data class AppMotion(
    val durationFast: Int = 120,
    val durationMedium: Int = 200,
    /** Réservé à l'apparition des listes (Lot 2 UX) ; pas encore consommé. */
    val durationSlow: Int = 300,
    val easeStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val easeEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val pressScale: Float = 0.97f,
    val enabled: Boolean = true,
) {
    /** Durée réelle à utiliser : 0 si le mouvement est désactivé. */
    fun effectiveDuration(base: Int): Int = if (enabled) base else 0
}
