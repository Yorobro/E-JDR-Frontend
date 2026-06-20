package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensions standardisées du design system (espacements, rayons, bordures).
 *
 * Conteneur immuable lu via [AppTheme] pour garantir une mise en page cohérente.
 *
 * @property xs Espacement très petit.
 * @property sm Espacement petit.
 * @property md Espacement moyen (défaut).
 * @property lg Espacement grand.
 * @property xl Espacement très grand.
 * @property radiusSm Rayon d'arrondi petit.
 * @property radiusMd Rayon d'arrondi moyen.
 * @property borderWidth Épaisseur de bordure au repos.
 * @property borderWidthFocused Épaisseur de bordure au focus.
 * @property iconSize Taille d'icône par défaut.
 */
data class AppDimens(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val radiusSm: Dp = 6.dp,
    val radiusMd: Dp = 10.dp,
    val borderWidth: Dp = 1.5.dp,
    val borderWidthFocused: Dp = 2.dp,
    val iconSize: Dp = 20.dp,
)
