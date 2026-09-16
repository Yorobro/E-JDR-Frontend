package eu.ejdr.presentation.features.campaign.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.presentation.shared.component.base.AppIconButton
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.organism.AppCard
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.util.formatDate

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
    AppCard(
        modifier = modifier.height(CardHeight),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = AppTheme.dimens.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppText(
                    text = campaign.name,
                    style = AppTextStyle.Subtitle,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
                AppText(
                    text = "Créée le ${formatDate(campaign.createdAt)}",
                    style = AppTextStyle.Caption,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            if (onDelete != null) {
                AppIconButton(
                    onClick = onDelete,
                    contentDescription = "Supprimer la campagne",
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    AppIcon(
                        imageVector = AppIcons.Delete,
                        contentDescription = null,
                        tint = AppTheme.colors.danger,
                    )
                }
            }
        }
    }
}
