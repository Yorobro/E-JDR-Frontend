package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.component.modifier.interactiveCard
import eu.ejdr.presentation.shared.component.modifier.interactiveCardElevation
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.AppTreatment

/**
 * Carte unique du design system — **seule** primitive de surface élevée de l'app.
 *
 * Généralise le motif éprouvé de la tuile de fiche : surface à fond `surface`,
 * bordure, coins arrondis et ombre douce, avec retour d'interaction partagé (scale au press +
 * élévation au survol). Remplace les trois implémentations divergentes qui coexistaient
 * (Box plat bordé, Surface élevé, Material Card) pour une apparence cohérente partout.
 *
 * Quand [onClick] est `null`, la carte n'est **pas** cliquable (pas de sémantique bouton, pas de
 * feedback press) — préserve l'accessibilité des tuiles décoratives. Quand [selected] est vrai, la
 * bordure passe en `primary` (état actif), sinon `border`.
 *
 * En traitement [AppTreatment.Rich], un filet doré intérieur subtil est ajouté autour du contenu.
 *
 * @param modifier Modifier appliqué à la carte (largeur, hauteur fixe, `animateItem`…).
 * @param onClick Action au clic ; si `null`, la carte n'est pas cliquable.
 * @param selected Met la bordure en accent `primary` (ex. groupe actif, élément sélectionné).
 * @param elevation Élévation au repos (par défaut `elevationMd`, ombre des tuiles de grille).
 * @param shape Forme de la carte (par défaut coins `radiusLg`).
 * @param containerColor Couleur de fond (par défaut `surface` ; `beige` pour les cartes denses).
 * @param contentPadding Marge intérieure autour de [content] ; `PaddingValues(0.dp)` pour gérer
 * soi-même l'alignement (ex. contenu centré).
 * @param content Contenu de la carte.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    elevation: Dp = AppTheme.dimens.elevationMd,
    shape: Shape = RoundedCornerShape(AppTheme.dimens.radiusLg),
    containerColor: Color = AppTheme.colors.surface,
    contentPadding: PaddingValues = PaddingValues(AppTheme.dimens.md),
    content: @Composable () -> Unit,
) {
    val enabled = onClick != null
    val interactionSource = remember { MutableInteractionSource() }
    val animatedElevation = interactiveCardElevation(interactionSource, enabled, base = elevation)
    val border = BorderStroke(
        AppTheme.dimens.borderWidth,
        if (selected) AppTheme.colors.primary else AppTheme.colors.border,
    )
    val surfaceModifier = modifier
        .fillMaxWidth()
        .interactiveCard(interactionSource, enabled)

    val isRich = AppTheme.treatment == AppTreatment.Rich
    val ornament = AppTheme.colors.ornament
    val dimens = AppTheme.dimens

    AppSurface(
        modifier = surfaceModifier,
        shape = shape,
        color = containerColor,
        contentColor = AppTheme.colors.text,
        border = border,
        elevation = if (onClick != null) animatedElevation else elevation,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        val contentModifier = if (isRich) {
            Modifier.drawBehind {
                val inset = dimens.xs.toPx()
                val radiusLg = dimens.radiusLg.toPx()
                val xs = dimens.xs.toPx()
                drawRoundRect(
                    color = ornament.copy(alpha = 0.22f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    cornerRadius = CornerRadius(radiusLg - xs),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        } else {
            Modifier
        }
        Box(contentModifier) {
            PaddedContent(contentPadding, content)
        }
    }
}

/** Applique la marge intérieure de la carte autour du contenu. */
@Composable
private fun PaddedContent(contentPadding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.padding(contentPadding)) { content() }
}
