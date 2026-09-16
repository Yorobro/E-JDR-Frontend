package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Atome case à cocher du design system.
 *
 * Composant bête : affiche un état coché/décoché avec un libellé cliquable et remonte
 * le changement. La ligne entière est cliquable (case + libellé). Les couleurs sont
 * lues dans le thème.
 *
 * @param checked État courant de la case.
 * @param onCheckedChange Callback déclenché lors d'un changement d'état.
 * @param label Libellé affiché à droite de la case.
 * @param modifier Modifier Compose appliqué à la ligne.
 * @param enabled Active ou désactive l'interaction.
 */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val boxSize = 20.dp
    val borderColor = if (enabled) colors.border else colors.beige
    val fillColor = when {
        !enabled -> colors.beige
        checked -> colors.primary
        else -> Color.Transparent
    }
    val checkmarkColor = colors.onPrimary

    Row(
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.sm),
    ) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(dimens.radiusSm))
                .drawBehind {
                    val cornerRadius = CornerRadius(dimens.radiusSm.toPx())
                    // Fill background
                    drawRoundRect(color = fillColor, cornerRadius = cornerRadius)
                    // Border when unchecked or disabled
                    if (!checked || !enabled) {
                        drawRoundRect(
                            color = borderColor,
                            cornerRadius = cornerRadius,
                            style = Stroke(width = dimens.borderWidth.toPx()),
                            size = Size(size.width, size.height),
                        )
                    }
                },
        ) {
            if (checked) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val strokeWidth = 2.dp.toPx()
                    // Checkmark: two lines forming a tick (left arm + right arm)
                    drawLine(
                        color = checkmarkColor,
                        start = Offset(w * 0.2f, h * 0.5f),
                        end = Offset(w * 0.42f, h * 0.72f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = checkmarkColor,
                        start = Offset(w * 0.42f, h * 0.72f),
                        end = Offset(w * 0.8f, h * 0.28f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        AppText(
            text = label,
            style = AppTextStyle.Body,
            color = if (enabled) colors.text else colors.muted,
        )
    }
}
