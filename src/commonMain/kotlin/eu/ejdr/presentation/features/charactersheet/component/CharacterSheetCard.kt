package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.modifier.interactiveCard
import eu.ejdr.presentation.shared.component.modifier.interactiveCardElevation
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.util.formatDate

private val CardHeight = 140.dp

/**
 * Tuile d'une fiche de personnage dans la grille (composant bête).
 *
 * Tuile à hauteur fixe (fond `surface`, bordure, coins arrondis, ombre douce) : nom et date de
 * création alignés en haut, icônes optionnelles de copie puis de suppression en coin haut-droite.
 * Quand [onClick] est `null`, la tuile n'est pas cliquable (ex. liste en lecture seule du détail de
 * campagne) ; sinon toute la tuile est cliquable et ouvre le détail de la fiche. Quand [onCopy] ou
 * [onDelete] est `null`, l'icône correspondante n'est pas affichée (le clic sur une icône remonte
 * son callback sans déclencher [onClick]).
 *
 * @param sheet Fiche à afficher.
 * @param onClick Callback déclenché au clic sur la tuile (ouvre le détail) ; si `null`, la tuile n'est pas cliquable.
 * @param onCopy Callback déclenché au clic sur l'icône de copie ; si `null`, l'icône est masquée.
 * @param onDelete Callback déclenché au clic sur l'icône de suppression ; si `null`, l'icône est masquée.
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun CharacterSheetCard(
    sheet: CharacterSheet,
    onClick: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    val enabled = onClick != null
    val interactionSource = remember { MutableInteractionSource() }
    val elevation = interactiveCardElevation(interactionSource, enabled, base = AppTheme.dimens.elevationMd)
    val surfaceModifier = modifier
        .fillMaxWidth()
        .height(CardHeight)
        .interactiveCard(interactionSource, enabled)
    val border = BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border)

    val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppTheme.dimens.md),
            ) {
                AppText(
                    text = sheet.name,
                    style = AppTextStyle.Subtitle,
                    maxLines = 2,
                )
                AppText(
                    text = "Créée le ${formatDate(sheet.createdAt)}",
                    style = AppTextStyle.Caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
            if (onCopy != null || onDelete != null) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
                ) {
                    if (onCopy != null) {
                        IconButton(onClick = onCopy) {
                            AppIcon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copier la fiche",
                                tint = AppTheme.colors.textSecondary,
                            )
                        }
                    }
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
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            interactionSource = interactionSource,
            shape = shape,
            color = AppTheme.colors.surface,
            contentColor = AppTheme.colors.text,
            shadowElevation = elevation,
            border = border,
        ) { content() }
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = AppTheme.colors.surface,
            contentColor = AppTheme.colors.text,
            shadowElevation = AppTheme.dimens.elevationMd,
            border = border,
        ) { content() }
    }
}
