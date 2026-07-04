package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme

private val DefaultMarkSize = 72.dp

/**
 * Marque vectorielle de l'application — un fleuron « grimoire » dessiné au [Canvas].
 *
 * Losange central (sceau) cerné d'un double filet et flanqué de deux petits losanges, évoquant
 * une enluminure de manuscrit. Entièrement vectoriel (aucune image, aucun hex) et thémé : le filet
 * extérieur suit `border`, le sceau intérieur suit `primary`. Utilisé par l'écran de démarrage et,
 * en petit, comme accent d'en-tête.
 *
 * @param modifier Modifier Compose appliqué à la marque.
 * @param size Côté du carré de dessin (la marque est centrée dedans).
 */
@Composable
fun AppBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = DefaultMarkSize,
) {
    val primary = AppTheme.colors.primary
    val border = AppTheme.colors.border
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val rOuter = minOf(w, h) / 2f
        val rInner = rOuter * 0.55f
        val stroke = Stroke(width = rOuter * 0.06f)

        // Double filet en losange (le contour ornemental).
        drawPath(diamond(cx, cy, rOuter), color = border, style = stroke)
        drawPath(diamond(cx, cy, rOuter * 0.82f), color = border, style = stroke)

        // Sceau central plein (l'accent de la marque).
        drawPath(diamond(cx, cy, rInner), color = primary)

        // Deux petits losanges d'accent, à gauche et à droite du sceau.
        val satellite = rOuter * 0.12f
        drawPath(diamond(cx - rOuter * 0.7f, cy, satellite), color = primary)
        drawPath(diamond(cx + rOuter * 0.7f, cy, satellite), color = primary)
    }
}

/** Construit un chemin en losange (diamant) centré sur ([cx], [cy]) de demi-diagonale [r]. */
private fun diamond(cx: Float, cy: Float, r: Float): Path = Path().apply {
    moveTo(cx, cy - r)
    lineTo(cx + r, cy)
    lineTo(cx, cy + r)
    lineTo(cx - r, cy)
    close()
}
