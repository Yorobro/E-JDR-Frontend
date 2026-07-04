package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.LocalContentColor

/**
 * Surface stylée maison — remplace `androidx.compose.material3.Surface`.
 *
 * Peint un fond, une forme, une bordure optionnelle et une ombre douce, et propage
 * [contentColor] via [LocalContentColor]. Cliquable si [onClick] est fourni (feedback scale).
 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.surface,
    contentColor: Color = AppTheme.colors.text,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    var m = modifier
    if (elevation > 0.dp) {
        m = m.shadow(elevation = elevation, shape = shape, clip = false)
    }
    m = m.clip(shape).background(color = color, shape = shape)
    if (border != null) m = m.border(border, shape)
    if (onClick != null) {
        m = m.appPressFeedback(source).clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
        )
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        androidx.compose.foundation.layout.Box(m) { content() }
    }
}
