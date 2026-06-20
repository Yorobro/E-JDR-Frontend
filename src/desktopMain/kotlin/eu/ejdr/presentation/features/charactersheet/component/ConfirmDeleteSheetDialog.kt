package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.organism.AppDialog

/**
 * Boîte de dialogue de confirmation de suppression d'une fiche (composant bête, réutilise
 * [AppDialog] avec un bouton de confirmation destructif).
 *
 * @param sheetName Nom de la fiche à supprimer (affiché dans le message).
 * @param onConfirm Callback de confirmation.
 * @param onDismiss Callback d'annulation.
 * @param modifier Modifier Compose appliqué au dialog.
 */
@Composable
fun ConfirmDeleteSheetDialog(
    sheetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppDialog(
        title = "Supprimer la fiche",
        onDismiss = onDismiss,
        confirmLabel = "Supprimer",
        onConfirm = onConfirm,
        modifier = modifier,
        confirmVariant = ButtonVariant.Danger,
    ) {
        AppText("Supprimer « $sheetName » ? Cette action est irréversible.")
    }
}
