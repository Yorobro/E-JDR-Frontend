package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = AppTheme.colors.primary,
        contentColor = AppTheme.colors.onPrimary,
    ) {
        AppIcon(
            imageVector = Icons.Filled.Add,
            contentDescription = contentDescription,
            tint = AppTheme.colors.onPrimary,
        )
    }
}
