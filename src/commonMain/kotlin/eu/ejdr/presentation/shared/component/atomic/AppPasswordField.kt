package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import eu.ejdr.presentation.shared.component.base.AppIconButton
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Champ de saisie de mot de passe.
 *
 * S'appuie sur [AppTextField] en masquant les caractères et en ajoutant un bouton
 * de bascule de visibilité (œil). Le masquage est géré localement ; la valeur est
 * toujours remontée en clair via [onValueChange]. Composant bête.
 *
 * @param value Valeur courante.
 * @param onValueChange Callback déclenché à chaque modification.
 * @param label Libellé du champ.
 * @param modifier Modifier Compose appliqué au champ.
 * @param errorMessage Message d'erreur ; non nul met le champ en état d'erreur.
 * @param enabled Active ou désactive la saisie.
 * @param leadingIcon Icône optionnelle au début du champ.
 */
@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    var visible by remember { mutableStateOf(false) }
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        errorMessage = errorMessage,
        enabled = enabled,
        leadingIcon = leadingIcon,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingContent = {
            AppIconButton(
                onClick = { visible = !visible },
                contentDescription = if (visible) "Masquer le mot de passe" else "Afficher le mot de passe",
                enabled = enabled,
            ) {
                AppIcon(
                    imageVector = if (visible) AppIcons.VisibilityOff else AppIcons.Visibility,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                )
            }
        },
    )
}
