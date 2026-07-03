package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Champ de saisie « outlined » maison, bâti sur [BasicTextField] (foundation, sans Material).
 *
 * Gère la bordure (repos/focus/erreur/désactivé), le placeholder, le curseur et les
 * contenus d'en-tête/fin. Le libellé et le message d'erreur sont fournis par l'appelant
 * de plus haut niveau (`AppTextField`).
 */
@Composable
fun AppTextFieldCore(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        !enabled -> colors.beige
        isError -> colors.danger
        focused -> colors.primary
        else -> colors.border
    }
    val borderWidth = if (focused) dimens.borderWidthFocused else dimens.borderWidth
    val shape = RoundedCornerShape(dimens.radiusMd)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = AppTheme.typography.body.copy(color = if (enabled) colors.text else colors.muted),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = dimens.md, vertical = dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
                androidx.compose.foundation.layout.Spacer(Modifier.width(dimens.xs))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    AppText(placeholder, style = AppTextStyle.Body, color = colors.muted)
                }
                innerTextField()
            }
            if (trailingContent != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.width(dimens.xs))
                trailingContent()
            }
        }
    }
}
