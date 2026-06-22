package eu.ejdr.presentation.features.campaign.component

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
 * Boîte de dialogue de création d'une campagne (composant bête).
 *
 * Habille le modal réutilisable [AppDialog] avec un champ « nom ». La saisie est un état
 * d'UI local (sans logique métier) ; la validation réelle est faite par le serveur et son
 * éventuelle erreur est affichée via [errorMessage].
 *
 * @param onDismiss Callback de fermeture sans création.
 * @param onConfirm Callback de confirmation, portant le nom saisi.
 * @param modifier Modifier Compose appliqué au dialog.
 * @param errorMessage Message d'erreur à afficher sous le champ (ex. nom invalide).
 */
@Composable
fun CreateCampaignDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    var name by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }
    val fieldError = if (touched && name.isBlank()) "Le nom ne peut pas être vide" else null

    AppDialog(
        title = "Nouvelle campagne",
        onDismiss = onDismiss,
        confirmLabel = "Créer",
        onConfirm = { onConfirm(name.trim()) },
        modifier = modifier,
        confirmEnabled = name.isNotBlank(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AppTextField(
                value = name,
                onValueChange = { name = it; touched = true },
                label = "Nom de la campagne",
                errorMessage = fieldError,
                modifier = Modifier.fillMaxWidth(),
            )
            FormError(message = errorMessage)
        }
    }
}
