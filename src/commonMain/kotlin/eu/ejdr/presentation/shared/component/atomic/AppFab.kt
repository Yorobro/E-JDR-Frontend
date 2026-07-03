package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.icons.AppIcons
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Bouton d'action flottant (FAB) de la direction artistique du site (composant bête).
 *
 * Rond, fond `primary`, icône « + » centrée. Réutilisable par tout écran ayant une action
 * de création principale (placé en bas à droite par l'appelant via un `Modifier.align`).
 *
 * @param onClick Callback déclenché au clic.
 * @param contentDescription Description d'accessibilité de l'action (ex. « Ajouter une fiche »).
 * @param modifier Modifier Compose appliqué au bouton (alignement/padding fournis par l'appelant).
 */
@Composable
fun AppFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    AppSurface(
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        color = AppTheme.colors.primary,
        contentColor = AppTheme.colors.onPrimary,
        elevation = AppTheme.dimens.elevationLg,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                imageVector = AppIcons.Add,
                contentDescription = contentDescription,
                tint = AppTheme.colors.onPrimary,
            )
        }
    }
}
