package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.organism.AppCard
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
                CampaignCaption(sheet)
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

    AppCard(
        modifier = modifier.height(CardHeight),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        content = content,
    )
}

/**
 * Sous-titre de campagne d'une tuile : « En attente de validation » tant que le rattachement est
 * PENDING, sinon le nom de la campagne (indispensable pour distinguer les copies d'une même fiche).
 * N'affiche rien si la fiche ne porte pas d'info de campagne.
 */
@Composable
private fun CampaignCaption(sheet: CharacterSheet) {
    when {
        sheet.linkStatus == "PENDING" ->
            AppText(
                text = "En attente de validation",
                style = AppTextStyle.Caption,
                color = AppTheme.colors.muted,
            )

        !sheet.campaignName.isNullOrBlank() ->
            AppText(
                text = sheet.campaignName,
                style = AppTextStyle.Caption,
                color = AppTheme.colors.textSecondary,
            )
    }
}
