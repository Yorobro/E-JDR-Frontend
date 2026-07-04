package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.LocalContentColor

/**
 * Atome icône du design system.
 *
 * Composant bête : affiche une icône vectorielle à la taille et à la teinte du thème.
 * Centralise le dimensionnement et la couleur des icônes pour une apparence cohérente.
 *
 * @param imageVector Icône vectorielle à afficher.
 * @param contentDescription Description d'accessibilité ; `null` si l'icône est décorative.
 * @param modifier Modifier Compose appliqué à l'icône.
 * @param tint Teinte explicite ; si `null`, utilise [LocalContentColor] (héritage Compose).
 *   Par défaut `AppTheme` la fixe à `colors.text` ; les conteneurs (FAB, bouton…)
 *   la surchargent localement par leur `contentColor`, propagé automatiquement.
 * @param size Taille de l'icône (défaut : `AppTheme.dimens.iconSize`).
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = AppTheme.dimens.iconSize,
) {
    Image(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint ?: LocalContentColor.current),
    )
}
