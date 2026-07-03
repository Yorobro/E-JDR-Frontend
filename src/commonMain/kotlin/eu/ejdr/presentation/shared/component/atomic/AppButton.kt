package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.base.AppSpinner
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.AppTreatment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Fenêtre d'anti double-clic : un 2e clic dans cet intervalle après un clic accepté est ignoré. */
private const val CLICK_GUARD_WINDOW_MS = 400L

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
 * Inclut un retour visuel au press (léger scale animé via [AppTheme.motion], géré par
 * [AppSurface]) et un mécanisme anti double-clic (ignore les clics distants de moins de 400 ms).
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
    val dimens = AppTheme.dimens
    val shape = RoundedCornerShape(dimens.radiusMd)
    val isEnabled = enabled && !loading
    val isRich = AppTheme.treatment == AppTreatment.Rich

    // Press-feedback is delegated to AppSurface (via appPressFeedback) — no manual graphicsLayer here.
    val interactionSource = remember { MutableInteractionSource() }

    // Anti double-clic
    var clickable by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val guardedClick: () -> Unit = {
        if (clickable) {
            clickable = false
            onClick()
            scope.launch {
                delay(CLICK_GUARD_WINDOW_MS)
                clickable = true
            }
        }
    }

    val padding = PaddingValues(horizontal = dimens.lg, vertical = dimens.sm)

    // Determine per-variant surface properties
    val surfaceColor: Color
    val contentColor: Color
    val border: BorderStroke?
    val useGradient: Boolean

    when (variant) {
        ButtonVariant.Primary -> {
            useGradient = isRich && isEnabled
            surfaceColor = if (useGradient) Color.Transparent else if (isEnabled) colors.primary else colors.border
            contentColor = if (isEnabled) colors.onPrimary else colors.muted
            border = if (useGradient) BorderStroke(dimens.borderWidth, colors.ornament) else null
        }
        ButtonVariant.Secondary -> {
            useGradient = false
            surfaceColor = if (isEnabled) colors.surface else colors.border
            contentColor = if (isEnabled) colors.text else colors.muted
            border = BorderStroke(dimens.borderWidth, if (isEnabled) colors.border else colors.muted)
        }
        ButtonVariant.Text -> {
            useGradient = false
            surfaceColor = Color.Transparent
            contentColor = if (isEnabled) colors.primary else colors.muted
            border = null
        }
        ButtonVariant.Danger -> {
            useGradient = false
            surfaceColor = if (isEnabled) colors.danger else colors.border
            contentColor = if (isEnabled) colors.onDanger else colors.muted
            border = null
        }
        ButtonVariant.Ghost -> {
            useGradient = false
            surfaceColor = Color.Transparent
            contentColor = if (isEnabled) colors.textSecondary else colors.muted
            border = BorderStroke(dimens.borderWidth, if (isEnabled) colors.border else colors.muted)
        }
    }

    AppSurface(
        modifier = modifier,
        shape = shape,
        color = surfaceColor,
        contentColor = contentColor,
        border = border,
        onClick = if (isEnabled) guardedClick else null,
        interactionSource = interactionSource,
    ) {
        if (useGradient) {
            // Rich Primary: paint gradient inside, then layer the content on top
            val gradient = Brush.verticalGradient(
                listOf(colors.accentGradientTop, colors.accentGradientBottom),
            )
            Box(
                modifier = Modifier.fillMaxWidth().background(gradient).padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ButtonContent(loading = loading, leadingIcon = leadingIcon, label = label, contentColor = contentColor)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ButtonContent(loading = loading, leadingIcon = leadingIcon, label = label, contentColor = contentColor)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    loading: Boolean,
    leadingIcon: ImageVector?,
    label: String,
    contentColor: Color,
) {
    if (loading) {
        AppSpinner(size = 18.dp)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        ) {
            if (leadingIcon != null) {
                AppIcon(imageVector = leadingIcon, contentDescription = null, tint = contentColor)
            }
            AppText(text = label, style = AppTextStyle.Label, color = contentColor)
        }
    }
}
