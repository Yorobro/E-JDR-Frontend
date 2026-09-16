package eu.ejdr.presentation.shared.component.base

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Retour tactile maison (remplace le ripple Material) : léger scale au press.
 *
 * @param interactionSource Source d'interaction partagée avec le `clickable`.
 * @param enabled Si faux, aucun effet.
 */
@Composable
fun Modifier.appPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = AppTheme.motion
    // `motion.enabled` inclus : en reduced-motion l'élément ne doit pas « sauter » au scale cible
    // (aligné sur InteractiveCard). Sans lui, effectiveDuration=0 figerait l'échelle à pressScale.
    val target = if (enabled && pressed && motion.enabled) motion.pressScale else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(motion.effectiveDuration(motion.durationFast), easing = motion.easeStandard),
        label = "appPressFeedback",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
