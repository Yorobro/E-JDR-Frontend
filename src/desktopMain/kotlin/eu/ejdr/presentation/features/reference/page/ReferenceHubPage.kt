package eu.ejdr.presentation.features.reference.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.organism.AppCard
import eu.ejdr.presentation.shared.theme.AppTheme

/** Nombre de colonnes de la grille. Les 8 types se répartissent en 4 × 2. */
private const val Columns = 4

/**
 * Page hub « Mes éléments » : grille des catégories de référence. Cliquer une tuile ouvre la
 * liste de gestion de la catégorie correspondante. Composant bête : il ne fait que disposer les
 * types et remonter le clic.
 *
 * Les tuiles remplissent tout l'espace disponible : la grille est découpée en lignes de poids
 * égal (hauteur) et chaque tuile prend une fraction égale de la largeur, si bien qu'agrandir la
 * fenêtre agrandit les tuiles au lieu de laisser du vide autour.
 *
 * @param onOpenType Callback d'ouverture d'une catégorie (porte le slug du type).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun ReferenceHubPage(
    onOpenType: (slug: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = ReferenceType.entries.chunked(Columns)
    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        rows.forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                rowTypes.forEach { type ->
                    ReferenceTypeTile(
                        label = type.label,
                        onClick = { onOpenType(type.slug) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                // Complète la dernière ligne incomplète avec des espaceurs de même poids,
                // pour que les tuiles gardent la même largeur d'une ligne à l'autre.
                repeat(Columns - rowTypes.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Tuile d'une catégorie : libellé centré, cliquable, remplit la cellule qui lui est allouée. */
@Composable
private fun RowScope.ReferenceTypeTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
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
}
