package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import eu.ejdr.presentation.shared.component.base.AppTextFieldCore
import eu.ejdr.presentation.shared.component.molecule.LabeledField
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Atome champ de saisie texte du design system (style « outlined »).
 *
 * Brique de base de tous les champs (texte, mot de passe, nombre). Composant bête :
 * affiche une valeur, remonte chaque modification, et gère visuellement les états
 * focus, erreur et désactivé via les couleurs du thème.
 *
 * Le libellé est rendu au-dessus du champ et le message d'erreur en dessous via
 * [LabeledField] ; le champ lui-même ([AppTextFieldCore]) ne duplique pas ces éléments.
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
    LabeledField(label = label, modifier = modifier, errorMessage = errorMessage) {
        AppTextFieldCore(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            enabled = enabled,
            isError = errorMessage != null,
            singleLine = singleLine,
            leadingContent = leadingIcon?.let { icon ->
                { AppIcon(icon, contentDescription = null, tint = AppTheme.colors.muted) }
            },
            trailingContent = trailingContent,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
        )
    }
}
