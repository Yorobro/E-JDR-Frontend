package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Atome champ de saisie texte du design system (style « outlined »).
 *
 * Brique de base de tous les champs (texte, mot de passe, nombre). Composant bête :
 * affiche une valeur, remonte chaque modification, et gère visuellement les états
 * focus, erreur et désactivé via les couleurs du thème.
 *
 * @param value Valeur courante du champ.
 * @param onValueChange Callback déclenché à chaque modification.
 * @param label Libellé du champ.
 * @param modifier Modifier Compose appliqué au champ.
 * @param placeholder Texte indicatif affiché quand le champ est vide.
 * @param errorMessage Message d'erreur ; non nul met le champ en état d'erreur.
 * @param enabled Active ou désactive la saisie.
 * @param singleLine Force une seule ligne.
 * @param leadingIcon Icône optionnelle au début du champ.
 * @param trailingContent Contenu optionnel en fin de champ (ex. bouton).
 * @param visualTransformation Transformation visuelle (ex. masquage mot de passe).
 * @param keyboardOptions Options clavier (type de saisie).
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AppTheme.colors
    val isError = errorMessage != null
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = trailingContent,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
                disabledBorderColor = colors.beige,
                errorBorderColor = colors.danger,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.textSecondary,
                cursorColor = colors.primary,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                disabledTextColor = colors.muted,
            ),
        )
        if (isError) {
            AppText(
                text = errorMessage!!,
                style = AppTextStyle.Caption,
                color = colors.danger,
            )
        }
    }
}
