package eu.ejdr.presentation.features.session.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.organism.AppDialog

/**
 * Boîte de dialogue de confirmation du retrait d'un joueur du salon d'attente (composant bête).
 *
 * Même habillage que [ConfirmDeleteSessionDialog] : action destructive, d'où le bouton de
 * confirmation en variante [ButtonVariant.Danger]. Le retrait reste réversible (le MJ peut
 * reconvier le joueur), ce que le message rappelle pour ne pas dramatiser l'action.
 *
 * @param pseudo Pseudo du joueur à retirer (affiché dans le message).
 * @param onConfirm Callback de confirmation du retrait.
 * @param onDismiss Callback d'annulation.
 * @param modifier Modifier Compose appliqué au dialog.
 */
@Composable
fun ConfirmRemoveParticipantDialog(
    pseudo: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppDialog(
        title = "Retirer ce joueur",
        onDismiss = onDismiss,
        confirmLabel = "Retirer",
        onConfirm = onConfirm,
        modifier = modifier,
        confirmVariant = ButtonVariant.Danger,
    ) {
        AppText("Retirer « $pseudo » du salon d'attente ? Vous pourrez le reconvier ensuite.")
    }
}
