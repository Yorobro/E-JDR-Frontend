package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppTextField

/**
 * Molécule combinant un champ de saisie ([AppTextField]) dans une colonne pleine largeur.
 *
 * Composant réutilisable et sans dépendance métier : il sert de brique de base pour composer
 * des formulaires et garantit une mise en page cohérente des champs.
 *
 * @param value Valeur courante du champ.
 * @param onValueChange Callback déclenché à chaque modification de la saisie.
 * @param label Libellé affiché dans le champ.
 * @param modifier Modifier Compose appliqué à la colonne.
 * @param isPassword Si vrai, masque les caractères saisis (mode mot de passe).
 * @param enabled Active ou désactive la saisie.
 */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isPassword = isPassword,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
