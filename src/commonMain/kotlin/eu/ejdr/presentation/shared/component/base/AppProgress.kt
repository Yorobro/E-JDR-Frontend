package eu.ejdr.presentation.shared.component.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme

/** Spinner circulaire maison (remplace CircularProgressIndicator). */
@Composable
fun AppSpinner(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    val color = AppTheme.colors.primary
    Canvas(modifier.size(size).graphicsLayer { rotationZ = angle }) {
        drawArc(
            color = color, startAngle = 0f, sweepAngle = 270f, useCenter = false,
            style = Stroke(width = size.toPx() * 0.12f, cap = StrokeCap.Round),
        )
    }
}

/** Barre de progression déterminée maison (remplace LinearProgressIndicator). */
@Composable
fun AppProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.dimens.radiusSm)
    androidx.compose.foundation.layout.Box(
        modifier.fillMaxWidth().height(6.dp).clip(shape).background(colors.beige),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(colors.primary),
        )
    }
}
