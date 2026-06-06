package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Atome champ de saisie texte de l'application.
 *
 * Composant réutilisable et sans dépendance métier : il affiche une valeur et remonte chaque
 * modification via [onValueChange]. Il prend en charge le mode mot de passe (masquage des
 * caractères) sans logique additionnelle.
 *
 * @param value Valeur courante du champ.
 * @param onValueChange Callback déclenché à chaque modification de la saisie.
 * @param label Libellé affiché dans le champ.
 * @param modifier Modifier Compose appliqué au champ.
 * @param isPassword Si vrai, masque les caractères saisis (mode mot de passe).
 * @param enabled Active ou désactive la saisie.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier,
    )
}
