package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Carte d'une fiche de personnage (composant bête).
 *
 * Affiche le nom dans la direction artistique du site, avec une action de suppression
 * optionnelle à droite. Quand [onDelete] est `null`, l'icône n'est pas affichée (ex. liste
 * en lecture seule).
 *
 * @param sheet Fiche à afficher.
 * @param onDelete Callback de suppression ; si `null`, l'icône est masquée.
 * @param modifier Modifier Compose appliqué à la carte.
 */
@Composable
fun CharacterSheetCard(
    sheet: CharacterSheet,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .padding(AppTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(
            text = sheet.name,
            style = AppTextStyle.Subtitle,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                AppIcon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer la fiche",
                    tint = AppTheme.colors.danger,
                )
            }
        }
    }
}
