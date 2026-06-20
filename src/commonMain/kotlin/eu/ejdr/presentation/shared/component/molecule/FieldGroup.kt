package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Molécule de regroupement vertical de champs (ou de tout contenu de formulaire).
 *
 * Composant réutilisable et sans dépendance métier : il dispose ses enfants dans une colonne
 * pleine largeur avec un espacement homogène issu du design system. Sert à composer des
 * formulaires cohérents sans répéter la logique de mise en page.
 *
 * @param modifier Modifier Compose appliqué à la colonne.
 * @param spacing Espacement vertical entre les enfants (défaut : `AppTheme.dimens.md`).
 * @param content Champs ou éléments à empiler.
 */
@Composable
fun FieldGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = AppTheme.dimens.md,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        content()
    }
}
