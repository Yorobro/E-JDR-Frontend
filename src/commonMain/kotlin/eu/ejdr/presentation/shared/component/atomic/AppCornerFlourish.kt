package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme

private val FlourishHeight = 12.dp

/**
 * Fleuron ornemental horizontal — un double filet interrompu par un petit losange central, évoquant
 * un séparateur de manuscrit enluminé.
 *
 * Signature visuelle assumée du thème « grimoire ». **Non interactif, sans animation**, dessiné au
 * [Canvas] (aucune image, aucun hex) : filet en `border`, losange en `primary`. À placer en
 * séparateur d'en-tête, jamais dans un formulaire. Discret par construction (filet fin, faible
 * contraste) pour rester un accent et non du bruit.
 *
 * @param modifier Modifier Compose appliqué au fleuron (largeur, marges).
 */
@Composable
fun AppCornerFlourish(modifier: Modifier = Modifier) {
    val line = AppTheme.colors.border
    val accent = AppTheme.colors.primary
    Canvas(modifier = modifier.fillMaxWidth().height(FlourishHeight)) {
        val w = size.width
        val cy = size.height / 2f
        val gap = size.height * 0.9f
        val diamond = size.height * 0.4f
        val strokeW = size.height * 0.08f

        // Deux filets fins partant des bords vers le losange central, avec un espace au milieu.
        drawLine(line, Offset(0f, cy), Offset(w / 2f - gap, cy), strokeWidth = strokeW)
        drawLine(line, Offset(w / 2f + gap, cy), Offset(w, cy), strokeWidth = strokeW)

        // Petit losange central plein (l'accent du séparateur).
        val center = w / 2f
        drawPath(
            Path().apply {
                moveTo(center, cy - diamond)
                lineTo(center + diamond, cy)
                lineTo(center, cy + diamond)
                lineTo(center - diamond, cy)
                close()
            },
            color = accent,
        )
    }
}
