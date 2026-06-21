package eu.ejdr.presentation.features.campaign.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.modifier.interactiveCard
import eu.ejdr.presentation.shared.theme.AppTheme

private val CardHeight = 140.dp

/**
 * Tuile d'une campagne dans la grille (composant bête).
 *
 * Tuile à hauteur fixe (fond `surface`, bordure, coins arrondis) : nom centré, icône de
 * suppression en coin haut-droite. Toute la tuile est cliquable (ouvre le détail) ; le clic
 * sur l'icône de suppression remonte [onDelete] sans déclencher [onClick].
 *
 * @param campaign Campagne à afficher.
 * @param onClick Callback déclenché au clic sur la tuile (ouvre le détail).
 * @param onDelete Callback déclenché au clic sur l'icône de suppression.
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun CampaignCard(
    campaign: Campaign,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .interactiveCard(interactionSource, enabled = true)
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .clickable(interactionSource = interactionSource, indication = indication, onClick = onClick),
    ) {
        AppText(
            text = campaign.name,
            style = AppTextStyle.Subtitle,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                AppIcon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer la campagne",
                    tint = AppTheme.colors.danger,
                )
            }
        }
    }
}
