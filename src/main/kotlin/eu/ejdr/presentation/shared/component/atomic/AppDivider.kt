package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Atome séparateur horizontal du design system.
 *
 * Composant bête : trace une ligne fine pleine largeur dans la couleur de bordure du thème
 * pour séparer visuellement des blocs de contenu.
 *
 * @param modifier Modifier Compose appliqué au séparateur.
 */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = AppTheme.dimens.borderWidth,
        color = AppTheme.colors.border,
    )
}

/**
 * Espace vertical fixe, à intercaler entre deux composants dans une `Column`.
 *
 * @param height Hauteur de l'espace (défaut : `AppTheme.dimens.md`).
 */
@Composable
fun VerticalSpacer(height: Dp = AppTheme.dimens.md) {
    Spacer(Modifier.height(height))
}

/**
 * Espace horizontal fixe, à intercaler entre deux composants dans une `Row`.
 *
 * @param width Largeur de l'espace (défaut : `AppTheme.dimens.md`).
 */
@Composable
fun HorizontalSpacer(width: Dp = AppTheme.dimens.md) {
    Spacer(Modifier.width(width))
}
