package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme

/* ----------------------------------------------------------------------------------------- *
 * Briques de mise en page de la fiche : une grille responsive qui reproduit la disposition en
 * colonnes de la fiche papier, et se replie en pile quand la fenêtre devient trop étroite.
 * ----------------------------------------------------------------------------------------- */

/** En dessous de cette largeur, les colonnes d'une [ResponsiveColumns] se replient en pile. */
private val FoldThreshold = 720.dp

/**
 * Range plusieurs colonnes côte à côte, mais les empile verticalement si la largeur disponible
 * passe sous [foldBelow]. Chaque colonne reçoit une largeur de poids égal en mode étalé.
 *
 * @param modifier Modifier appliqué au conteneur.
 * @param foldBelow Largeur en deçà de laquelle on replie en pile (défaut [FoldThreshold]).
 * @param columns Les colonnes à disposer (chacune un composable).
 */
@Composable
fun ResponsiveColumns(
    modifier: Modifier = Modifier,
    foldBelow: Dp = FoldThreshold,
    columns: List<@Composable () -> Unit>,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = AppTheme.dimens.lg
        if (maxWidth < foldBelow) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                columns.forEach { column -> column() }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                columns.forEach { column ->
                    Column(modifier = Modifier.weight(1f)) { column() }
                }
            }
        }
    }
}

/**
 * Colonne verticale de cellules espacées (un sous-bloc d'une [ResponsiveColumns]).
 *
 * @param modifier Modifier appliqué à la colonne.
 * @param content Cellules de la colonne.
 */
@Composable
fun FieldColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        content()
    }
}
