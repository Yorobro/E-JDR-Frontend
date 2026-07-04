package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Brique de chargement « fantôme » : surface neutre qui pulse en opacité (shimmer doux).
 *
 * Respecte [AppTheme] : couleur dérivée des tokens, durée de pulsation issue de [AppTheme.motion]
 * (donc nulle si le mouvement est désactivé → opacité fixe). Composant bête.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
) {
    val motion = AppTheme.motion
    val pulseDuration = motion.effectiveDuration(motion.durationSlow) * 2 // cycle lent
    val alpha = if (pulseDuration == 0) {
        0.4f
    } else {
        val transition = rememberInfiniteTransition(label = "skeletonPulse")
        val animated by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(pulseDuration, easing = motion.easeStandard),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeletonAlpha",
        )
        animated
    }
    Box(
        modifier = modifier
            .clip(shape)
            .alpha(alpha)
            .background(AppTheme.colors.border),
    )
}
