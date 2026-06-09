package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme

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
            onClick = onClick, enabled = isEnabled, shape = shape, modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
        ) { content(colors.onPrimary) }

        ButtonVariant.Danger -> Button(
            onClick = onClick, enabled = isEnabled, shape = shape, modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = colors.onDanger),
        ) { content(colors.onDanger) }

        ButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick, enabled = isEnabled, shape = shape, modifier = modifier,
            border = BorderStroke(AppTheme.dimens.borderWidth, colors.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        ) { content(colors.primary) }

        ButtonVariant.Text -> TextButton(
            onClick = onClick, enabled = isEnabled, shape = shape, modifier = modifier,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
        ) { content(colors.primary) }

        ButtonVariant.Ghost -> Button(
            onClick = onClick, enabled = isEnabled, shape = shape, modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = colors.beige, contentColor = colors.text),
        ) { content(colors.text) }
    }
}
