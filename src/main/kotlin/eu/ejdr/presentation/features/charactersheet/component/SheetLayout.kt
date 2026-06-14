package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/**
 * Rangée de bloc « caractéristiques » à hauteurs pondérées, fidèle à la fiche papier :
 *
 * - **Colonne 1** : [left] sur toute la hauteur de la rangée.
 * - **Colonne 2** : [middleTop] (2/3) au-dessus de [middleBottom] (1/3).
 * - **Colonne 3** : [rightTop] (1/2) au-dessus de [rightBottom] (1/2).
 *
 * La hauteur de la rangée s'aligne sur la colonne la plus haute (`IntrinsicSize.Min`), et les
 * blocs se répartissent cette hauteur via des poids verticaux. Sous [foldBelow], tout est empilé
 * en une seule colonne à hauteur naturelle (les ratios ne s'appliquent qu'en mode large).
 *
 * Chaque slot reçoit un [Modifier] à appliquer à sa carte (pour propager poids et hauteur).
 *
 * @param left Carte de gauche (pleine hauteur).
 * @param middleTop Carte du milieu, en haut (2/3).
 * @param middleBottom Carte du milieu, en bas (1/3).
 * @param rightTop Carte de droite, en haut (1/2).
 * @param rightBottom Carte de droite, en bas (1/2).
 * @param modifier Modifier appliqué au conteneur.
 * @param foldBelow Largeur en deçà de laquelle on replie en pile (défaut [FoldThreshold]).
 */
@Composable
fun StatBlockRow(
    left: @Composable (Modifier) -> Unit,
    middleTop: @Composable (Modifier) -> Unit,
    middleBottom: @Composable (Modifier) -> Unit,
    rightTop: @Composable (Modifier) -> Unit,
    rightBottom: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    foldBelow: Dp = FoldThreshold,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = AppTheme.dimens.lg
        if (maxWidth < foldBelow) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                left(Modifier)
                middleTop(Modifier)
                middleBottom(Modifier)
                rightTop(Modifier)
                rightBottom(Modifier)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                ColumnCell(weight = 1f) { left(Modifier.fillMaxHeight()) }
                StackedColumn(weight = 1f, gap = gap) {
                    middleTop(Modifier.weight(2f))
                    middleBottom(Modifier.weight(1f))
                }
                StackedColumn(weight = 1f, gap = gap) {
                    rightTop(Modifier.weight(1f))
                    rightBottom(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Une colonne de la rangée occupant [weight] de la largeur, pleine hauteur. */
@Composable
private fun RowScope.ColumnCell(weight: Float, content: @Composable () -> Unit) {
    Column(modifier = Modifier.weight(weight).fillMaxHeight()) { content() }
}

/** Une colonne empilant ses cartes (poids verticaux fournis par l'appelant), pleine hauteur. */
@Composable
private fun RowScope.StackedColumn(
    weight: Float,
    gap: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.weight(weight).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}
