package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import eu.ejdr.presentation.shared.theme.AppColors
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Tonalité d'un [AppBadge] : détermine fond et texte (dérivés des rôles [AppColors], aucun hex).
 *
 * - [Neutral] : pastille discrète (fond `beige`) — rôles, métadonnées.
 * - [Accent] : met en avant (fond `primary`) — état actif, mise en valeur.
 * - [Danger] : alerte (fond `danger`) — état bloquant, attention.
 */
enum class BadgeTone { Neutral, Accent, Danger }

/**
 * Petite pastille de statut/rôle du design system.
 *
 * Conteneur compact à coins arrondis (`radiusSm`) portant un libellé court en style `Label`.
 * Remplace les pastilles dessinées à la main (ex. rôle de membre). Non interactif.
 *
 * @param text Libellé affiché (court : rôle, statut…).
 * @param modifier Modifier Compose appliqué à la pastille.
 * @param tone Tonalité (fond + couleur du texte) ; [BadgeTone.Neutral] par défaut.
 */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
) {
    val colors = AppTheme.colors
    val background: Color = when (tone) {
        BadgeTone.Neutral -> colors.beige
        BadgeTone.Accent -> colors.primary
        BadgeTone.Danger -> colors.danger
    }
    val foreground: Color = when (tone) {
        BadgeTone.Neutral -> colors.text
        BadgeTone.Accent -> colors.onPrimary
        BadgeTone.Danger -> colors.onDanger
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppTheme.dimens.radiusSm))
            .background(background)
            .padding(horizontal = AppTheme.dimens.sm, vertical = AppTheme.dimens.xs),
    ) {
        AppText(text = text, style = AppTextStyle.Label, color = foreground)
    }
}
