package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

private val CardHeight = 140.dp

/**
 * Tuile d'une fiche de personnage dans la grille (composant bête).
 *
 * Tuile à hauteur fixe (fond `surface`, bordure, coins arrondis) : nom centré, icône de
 * suppression optionnelle en coin haut-droite. Quand [onClick] est `null`, la tuile n'est pas
 * cliquable (ex. liste en lecture seule du détail de campagne) ; sinon toute la tuile est
 * cliquable et ouvre le détail de la fiche. Quand [onDelete] est `null`, l'icône de suppression
 * n'est pas affichée (le clic sur l'icône remonte [onDelete] sans déclencher [onClick]).
 *
 * @param sheet Fiche à afficher.
 * @param onClick Callback déclenché au clic sur la tuile (ouvre le détail) ; si `null`, la tuile n'est pas cliquable.
 * @param onDelete Callback déclenché au clic sur l'icône de suppression ; si `null`, l'icône est masquée.
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun CharacterSheetCard(
    sheet: CharacterSheet,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    val base = modifier
        .fillMaxWidth()
        .height(CardHeight)
        .clip(shape)
        .background(AppTheme.colors.surface)
        .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
    ) {
        AppText(
            text = sheet.name,
            style = AppTextStyle.Subtitle,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                AppIcon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer la fiche",
                    tint = AppTheme.colors.danger,
                )
            }
        }
    }
}
