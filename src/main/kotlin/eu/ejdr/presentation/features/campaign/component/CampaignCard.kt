package eu.ejdr.presentation.features.campaign.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Carte d'une campagne dans la liste (composant bête).
 *
 * Affiche le nom de la campagne dans la direction artistique du site (fond `surface`, coins
 * arrondis, bordure). Toute la carte est cliquable (ouverture du détail) ; une icône de
 * suppression à droite remonte [onDelete] sans déclencher [onClick].
 *
 * @param campaign Campagne à afficher.
 * @param onClick Callback déclenché au clic sur la carte (ouvre le détail).
 * @param onDelete Callback déclenché au clic sur l'icône de suppression.
 * @param modifier Modifier Compose appliqué à la carte.
 */
@Composable
fun CampaignCard(
    campaign: Campaign,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .clickable(onClick = onClick)
            .padding(AppTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // `weight(1f)` laisse la place à l'icône : un nom long ne pousse pas le bouton hors carte.
        AppText(
            text = campaign.name,
            style = AppTextStyle.Subtitle,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            AppIcon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Supprimer la campagne",
                tint = AppTheme.colors.danger,
            )
        }
    }
}
