package eu.ejdr.presentation.shared.component.modifier

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Retour d'interaction partagé des cartes : **scale au press** (l'élévation hover/press est
 * fournie séparément par [interactiveCardElevation]).
 *
 * Source unique de l'animation des cartes (pas de copie dans chaque carte). Lit les tokens
 * [AppTheme.motion]. Le press n'est animé que si [enabled] (carte cliquable)
 * — préserve l'accessibilité des tuiles non cliquables.
 */
@Composable
fun Modifier.interactiveCard(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val motion = AppTheme.motion
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed && motion.enabled) motion.pressScale else 1f,
        animationSpec = tween(motion.effectiveDuration(motion.durationFast), easing = motion.easeStandard),
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Élévation animée à appliquer à la Surface de la carte (hover/press → variation douce). */
@Composable
fun interactiveCardElevation(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    base: Dp,
): Dp {
    val motion = AppTheme.motion
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val target: Dp = when {
        !enabled -> base
        pressed -> AppTheme.dimens.elevationSm
        hovered -> base + AppTheme.dimens.elevationSm
        else -> base
    }
    val elevation by animateDpAsState(
        targetValue = target,
        animationSpec = tween(motion.effectiveDuration(motion.durationFast), easing = motion.easeStandard),
    )
    return elevation
}
