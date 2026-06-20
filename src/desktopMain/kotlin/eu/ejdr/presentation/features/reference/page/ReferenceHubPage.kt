package eu.ejdr.presentation.features.reference.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

private val MinTileWidth = 180.dp
private val TileHeight = 120.dp

/**
 * Page hub « Mes éléments » : grille des six catégories de référence. Cliquer une tuile ouvre la
 * liste de gestion de la catégorie correspondante. Composant bête : il ne fait que disposer les
 * types et remonter le clic.
 *
 * @param onOpenType Callback d'ouverture d'une catégorie (porte le slug du type).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun ReferenceHubPage(
    onOpenType: (slug: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = MinTileWidth),
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.xl),
        contentPadding = PaddingValues(vertical = AppTheme.dimens.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        items(ReferenceType.entries, key = { it.slug }) { type ->
            ReferenceTypeTile(label = type.label, onClick = { onOpenType(type.slug) })
        }
    }
}

/** Tuile d'une catégorie : libellé centré, cliquable. */
@Composable
private fun ReferenceTypeTile(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(TileHeight)
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .clickable(onClick = onClick),
    ) {
        AppText(
            text = label,
            style = AppTextStyle.Subtitle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
        )
    }
}
