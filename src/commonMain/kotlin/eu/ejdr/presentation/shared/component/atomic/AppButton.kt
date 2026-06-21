package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Variantes visuelles de [AppButton]. */
enum class ButtonVariant { Primary, Secondary, Text, Danger, Ghost }

/**
 * Atome bouton du design system.
 *
 * Composant bête : affiche un libellé (et une icône optionnelle) et remonte le clic.
 * L'apparence dépend de [variant] et lit les couleurs du thème. Pendant [loading],
 * un indicateur remplace le contenu et le bouton est désactivé. La largeur se règle
 * via [modifier] (ex. `Modifier.fillMaxWidth()`).
 *
 * Inclut un retour visuel au press (léger scale animé via [AppTheme.motion]) et un
 * mécanisme anti double-clic (ignore les clics distants de moins de 400 ms).
 *
 * @param label Texte du bouton.
 * @param onClick Callback déclenché au clic.
 * @param modifier Modifier Compose appliqué au bouton.
 * @param variant Variante visuelle (défaut [ButtonVariant.Primary]).
 * @param enabled Active ou désactive le bouton.
 * @param loading Si vrai, affiche un indicateur de chargement et désactive le bouton.
 * @param leadingIcon Icône optionnelle affichée avant le libellé.
 */
@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    val isEnabled = enabled && !loading

    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = AppTheme.motion
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) motion.pressScale else 1f,
        animationSpec = tween(motion.effectiveDuration(motion.durationFast), easing = motion.easeStandard),
        label = "buttonPressScale",
    )
    val scaledModifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }

    // Anti double-clic
    var clickable by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val guardedClick: () -> Unit = {
        if (clickable) {
            clickable = false
            onClick()
            scope.launch {
                delay(400L)
                clickable = true
            }
        }
    }

    @Composable
    fun content(contentColor: Color) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = contentColor)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm)) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(AppTheme.dimens.iconSize))
                }
                AppText(text = label, style = AppTextStyle.Label, color = contentColor)
            }
        }
    }

    when (variant) {
        ButtonVariant.Primary -> Button(
            onClick = guardedClick, enabled = isEnabled, shape = shape, modifier = scaledModifier,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.border,
                disabledContentColor = colors.muted,
            ),
        ) { content(if (isEnabled) colors.onPrimary else colors.muted) }

        ButtonVariant.Danger -> Button(
            onClick = guardedClick, enabled = isEnabled, shape = shape, modifier = scaledModifier,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.danger,
                contentColor = colors.onDanger,
                disabledContainerColor = colors.border,
                disabledContentColor = colors.muted,
            ),
        ) { content(if (isEnabled) colors.onDanger else colors.muted) }

        ButtonVariant.Secondary -> OutlinedButton(
            onClick = guardedClick, enabled = isEnabled, shape = shape, modifier = scaledModifier,
            interactionSource = interactionSource,
            border = BorderStroke(AppTheme.dimens.borderWidth, if (isEnabled) colors.primary else colors.muted),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.primary,
                disabledContentColor = colors.muted,
            ),
        ) { content(if (isEnabled) colors.primary else colors.muted) }

        ButtonVariant.Text -> TextButton(
            onClick = guardedClick, enabled = isEnabled, shape = shape, modifier = scaledModifier,
            interactionSource = interactionSource,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.primary,
                disabledContentColor = colors.muted,
            ),
        ) { content(if (isEnabled) colors.primary else colors.muted) }

        ButtonVariant.Ghost -> Button(
            onClick = guardedClick, enabled = isEnabled, shape = shape, modifier = scaledModifier,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.beige,
                contentColor = colors.text,
                disabledContainerColor = colors.border,
                disabledContentColor = colors.muted,
            ),
        ) { content(if (isEnabled) colors.text else colors.muted) }
    }
}
