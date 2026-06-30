package eu.ejdr.presentation.shared.component.modifier

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Spec d'**apparition** des éléments de liste/grille, dérivée des tokens [AppTheme.motion].
 *
 * À passer en `fadeInSpec`/`placementSpec` de `Modifier.animateItem` pour une entrée en fondu
 * douce et cohérente (durée `durationSlow`, courbe `easeEmphasized`). Respecte la réduction de
 * mouvement : durée 0 quand le mouvement est désactivé. Centralise le motion des listes plutôt que
 * de le répéter à chaque page.
 */
@Composable
@ReadOnlyComposable
fun appItemAppearSpec(): FiniteAnimationSpec<Float> {
    val motion = AppTheme.motion
    return tween(
        durationMillis = motion.effectiveDuration(motion.durationSlow),
        easing = motion.easeEmphasized,
    )
}
