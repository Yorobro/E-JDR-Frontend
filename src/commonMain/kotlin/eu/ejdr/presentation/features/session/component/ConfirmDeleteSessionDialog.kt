package eu.ejdr.presentation.features.session.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.organism.AppDialog

/**
 * Boîte de dialogue de confirmation de suppression d'une session (composant bête).
 *
 * Habille le modal réutilisable [AppDialog] : action destructive, d'où le bouton de
 * confirmation en variante [ButtonVariant.Danger].
 *
 * @param sessionTitle Titre de la session à supprimer (affiché dans le message).
 * @param onConfirm Callback de confirmation de la suppression.
 * @param onDismiss Callback d'annulation.
 * @param modifier Modifier Compose appliqué au dialog.
 */
@Composable
fun ConfirmDeleteSessionDialog(
    sessionTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppDialog(
        title = "Supprimer la session",
        onDismiss = onDismiss,
        confirmLabel = "Supprimer",
        onConfirm = onConfirm,
        modifier = modifier,
        confirmVariant = ButtonVariant.Danger,
    ) {
        AppText("Supprimer « $sessionTitle » ? Cette action est irréversible.")
    }
}
