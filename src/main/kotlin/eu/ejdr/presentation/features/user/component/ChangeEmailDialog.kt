package eu.ejdr.presentation.features.user.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppDialog

/**
 * Boîte de dialogue de changement d'adresse e-mail (composant bête).
 *
 * Habille [AppDialog] avec un champ « Nouvel email ». La saisie est un état d'UI local ;
 * le résultat et une éventuelle erreur serveur sont gérés par le ViewModel parent.
 *
 * @param onDismiss Callback de fermeture sans confirmation.
 * @param onConfirm Callback de confirmation portant le nouvel email saisi.
 * @param modifier Modifier Compose appliqué au dialog.
 * @param errorMessage Message d'erreur à afficher (ex. email déjà utilisé).
 */
@Composable
fun ChangeEmailDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    var email by remember { mutableStateOf("") }

    AppDialog(
        title = "Changer d'email",
        onDismiss = onDismiss,
        confirmLabel = "Changer",
        onConfirm = { onConfirm(email.trim()) },
        modifier = modifier,
        confirmEnabled = email.isNotBlank(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = "Nouvel email",
                modifier = Modifier.fillMaxWidth(),
            )
            FormError(message = errorMessage)
        }
    }
}
