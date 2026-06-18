package eu.ejdr.presentation.features.reference.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.component.organism.AppDialog
import eu.ejdr.presentation.shared.theme.AppTheme

private val CardHeight = 120.dp

/**
 * Tuile d'un élément de référence dans la grille de gestion (composant bête) : nom centré + icône
 * de suppression. Clone de `CampaignCard`, sans clic d'ouverture (les éléments n'ont pas de détail).
 *
 * @param item Élément à afficher.
 * @param onDelete Callback de suppression.
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun ReferenceCard(
    item: ReferenceItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape),
    ) {
        AppText(
            text = item.name,
            style = AppTextStyle.Subtitle,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
        )
        IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
            AppIcon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Supprimer",
                tint = AppTheme.colors.danger,
            )
        }
    }
}

/**
 * Dialog de création d'un élément de référence (composant bête, clone de `CreateCampaignDialog`).
 *
 * @param title Titre du dialog (ex. « Nouvelle formation »).
 * @param label Libellé du champ nom.
 * @param onDismiss Fermeture sans création.
 * @param onConfirm Confirmation, portant le nom saisi.
 * @param errorMessage Message d'erreur éventuel (ex. doublon).
 */
@Composable
fun CreateReferenceDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    errorMessage: String? = null,
) {
    var name by remember { mutableStateOf("") }
    AppDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = "Créer",
        onConfirm = { onConfirm(name.trim()) },
        confirmEnabled = name.isNotBlank(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
            FormError(message = errorMessage)
        }
    }
}

/**
 * Dialog de confirmation de suppression d'un élément (action destructive).
 *
 * @param itemName Nom de l'élément (affiché dans le message).
 * @param onConfirm Confirmation de la suppression.
 * @param onDismiss Annulation.
 */
@Composable
fun ConfirmDeleteReferenceDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        title = "Supprimer l'élément",
        onDismiss = onDismiss,
        confirmLabel = "Supprimer",
        onConfirm = onConfirm,
        confirmVariant = ButtonVariant.Danger,
    ) {
        AppText("Supprimer « $itemName » ? Il sera retiré des fiches qui l'utilisent.")
    }
}
