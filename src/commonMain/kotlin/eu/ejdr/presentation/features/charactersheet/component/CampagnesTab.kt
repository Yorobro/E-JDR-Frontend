package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

private val MinCardWidth = 220.dp

/**
 * Onglet Campagnes : grille adaptative NON LAZY de cartes lecture seule (une par campagne).
 * Non lazy car le contenu d'onglet est déjà dans un `verticalScroll` (un `LazyVerticalGrid`
 * imbriqué planterait). Le nombre de colonnes suit la largeur, puis les cartes sont disposées
 * par rangées (Row + weight), à l'image de [ResponsiveColumns].
 *
 * @param campaigns Campagnes rattachées à la fiche (peut être vide).
 */
@Composable
fun CampagnesTab(campaigns: List<SheetCampaign>) {
    if (campaigns.isEmpty()) {
        AppText(
            text = "Cette fiche n'est rattachée à aucune campagne.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.textSecondary,
        )
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = (maxWidth / MinCardWidth).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md)) {
            campaigns.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                ) {
                    rowItems.forEach { campaign ->
                        SheetCampaignCard(campaign = campaign, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Carte lecture seule d'une campagne : nom (titre) + pseudo du MJ (sous-titre, secondaire) + badge
 * « En attente de validation » quand le rattachement est PENDING. Non cliquable, sans suppression.
 * Style aligné sur [SheetCard].
 *
 * @param campaign Campagne à afficher.
 * @param modifier Modifier Compose appliqué à la carte.
 */
@Composable
fun SheetCampaignCard(campaign: SheetCampaign, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
    ) {
        AppText(text = campaign.campaignName, style = AppTextStyle.Subtitle)
        AppText(
            text = campaign.gameMasterPseudo,
            style = AppTextStyle.Caption,
            color = AppTheme.colors.textSecondary,
        )
        if (campaign.linkStatus == "PENDING") {
            AppText(
                text = "En attente de validation",
                style = AppTextStyle.Caption,
                color = AppTheme.colors.muted,
            )
        }
    }
}
