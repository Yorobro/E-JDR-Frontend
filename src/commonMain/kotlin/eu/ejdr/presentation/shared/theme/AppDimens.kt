package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensions standardisées du design system (espacements, rayons, bordures, élévations).
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
 * @property radiusLg Rayon d'arrondi grand (cartes unifiées, dialogues) — touche premium plus douce.
 * @property radiusXl Rayon d'arrondi très grand (surfaces de marque : splash, tuiles signature).
 * @property borderWidth Épaisseur de bordure au repos.
 * @property borderWidthFocused Épaisseur de bordure au focus.
 * @property iconSize Taille d'icône par défaut.
 * @property elevationSm Élévation faible (ombre subtile pour sections).
 * @property elevationMd Élévation moyenne (ombre douce pour tuiles de grille).
 * @property elevationLg Élévation forte (vrai « lift » : carte survolée, splash).
 */
data class AppDimens(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 16.dp,
    val radiusXl: Dp = 20.dp,
    val borderWidth: Dp = 1.5.dp,
    val borderWidthFocused: Dp = 2.dp,
    val iconSize: Dp = 20.dp,
    val elevationSm: Dp = 1.dp,
    val elevationMd: Dp = 3.dp,
    val elevationLg: Dp = 6.dp,
)
