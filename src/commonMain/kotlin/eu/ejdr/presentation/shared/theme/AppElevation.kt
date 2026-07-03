package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Jetons d'ombre maison (Material ne fournit plus l'élévation).
 *
 * Chaque niveau décrit un décalage vertical, un rayon de flou et une couleur d'ombre.
 * Consommés par `AppSurface` pour peindre une ombre douce derrière les cartes.
 *
 * @property color Couleur de base de l'ombre (noir semi-transparent).
 * @property offsetSm/md/lg Décalage vertical par niveau.
 * @property blurSm/md/lg Rayon de flou par niveau.
 */
data class AppElevation(
    val color: Color = Color(0x33000000),
    val offsetSm: Dp = 1.dp,
    val offsetMd: Dp = 3.dp,
    val offsetLg: Dp = 8.dp,
    val blurSm: Dp = 3.dp,
    val blurMd: Dp = 10.dp,
    val blurLg: Dp = 24.dp,
)
