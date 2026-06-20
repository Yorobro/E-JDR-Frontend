package eu.ejdr.presentation.features.user.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppPasswordField
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppDialog

/**
 * Boîte de dialogue de changement de mot de passe (composant bête).
 *
 * Habille [AppDialog] avec deux champs [AppPasswordField] (mot de passe actuel et nouveau).
 * La saisie est un état d'UI local ; le résultat et une éventuelle erreur serveur sont gérés
 * par le ViewModel parent.
 *
 * @param onDismiss Callback de fermeture sans confirmation.
 * @param onConfirm Callback de confirmation portant (mot de passe actuel, nouveau mot de passe).
 * @param modifier Modifier Compose appliqué au dialog.
 * @param errorMessage Message d'erreur à afficher (ex. identifiants invalides).
 */
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AppDialog(
        title = "Changer le mot de passe",
        onDismiss = onDismiss,
        confirmLabel = "Changer",
        onConfirm = { onConfirm(currentPassword, newPassword) },
        modifier = modifier,
        confirmEnabled = currentPassword.isNotBlank() && newPassword.isNotBlank(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AppPasswordField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Mot de passe actuel",
                modifier = Modifier.fillMaxWidth(),
            )
            AppPasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "Nouveau mot de passe",
                modifier = Modifier.fillMaxWidth(),
            )
            FormError(message = errorMessage)
        }
    }
}
