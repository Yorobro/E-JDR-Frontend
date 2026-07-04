package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.SkeletonBox
import eu.ejdr.presentation.shared.theme.AppTheme

/** Grille de tuiles « fantômes » pendant le chargement initial (même disposition adaptative). */
@Composable
fun SkeletonGrid(
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    count: Int = 6,
    minTileWidth: Dp = 180.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minTileWidth),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        items(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(itemHeight))
        }
    }
}

/** Liste de rangées « fantômes » pendant le chargement initial (pour les LazyColumn). */
@Composable
fun SkeletonList(
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    count: Int = 5,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        repeat(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(itemHeight))
        }
    }
}
