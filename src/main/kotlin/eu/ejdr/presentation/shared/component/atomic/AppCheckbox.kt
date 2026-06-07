package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
    Row(
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.primary,
                uncheckedColor = colors.border,
                checkmarkColor = colors.onPrimary,
                disabledCheckedColor = colors.beige,
                disabledUncheckedColor = colors.beige,
            ),
        )
        AppText(
            text = label,
            style = AppTextStyle.Body,
            color = if (enabled) colors.text else colors.muted,
        )
    }
}
