package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import eu.ejdr.presentation.shared.component.modifier.interactiveCard
import eu.ejdr.presentation.shared.component.modifier.interactiveCardElevation
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Carte unique du design system — **seule** primitive de surface élevée de l'app.
 *
 * Généralise le motif éprouvé de la tuile de fiche : `Surface` Material3 à fond `surface`,
 * bordure, coins arrondis et ombre douce, avec retour d'interaction partagé (scale au press +
 * élévation au survol). Remplace les trois implémentations divergentes qui coexistaient
 * (Box plat bordé, Surface élevé, Material Card) pour une apparence cohérente partout.
 *
 * Quand [onClick] est `null`, la carte n'est **pas** cliquable (pas de sémantique bouton, pas de
 * feedback press) — préserve l'accessibilité des tuiles décoratives. Quand [selected] est vrai, la
 * bordure passe en `primary` (état actif), sinon `border`.
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
    val padded: @Composable () -> Unit = { PaddedContent(contentPadding, content) }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            interactionSource = interactionSource,
            shape = shape,
            color = containerColor,
            contentColor = AppTheme.colors.text,
            shadowElevation = animatedElevation,
            border = border,
            content = padded,
        )
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = containerColor,
            contentColor = AppTheme.colors.text,
            shadowElevation = elevation,
            border = border,
            content = padded,
        )
    }
}

/** Applique la marge intérieure de la carte autour du contenu. */
@Composable
private fun PaddedContent(contentPadding: PaddingValues, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.padding(contentPadding)) { content() }
}
