package eu.ejdr.presentation.features.campaign.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Boîte de dialogue de sélection d'une fiche à rattacher à une campagne (composant bête).
 *
 * Affiche la liste des fiches sélectionnables ; le clic sur une fiche déclenche [onSelect].
 * S'appuie sur `AlertDialog` directement (contenu = liste cliquable, pas le couple
 * confirmer/annuler standard d'`AppDialog`).
 *
 * @param sheets Fiches rattachables renvoyées par le back (autres joueurs, non déjà liées).
 * @param onSelect Callback portant l'identifiant de la fiche choisie.
 * @param onDismiss Callback de fermeture.
 * @param modifier Modifier Compose appliqué au dialog.
 */
@Composable
fun LinkCharacterDialog(
    sheets: List<CharacterSheet>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { AppText("Rattacher une fiche", style = AppTextStyle.Title) },
        text = {
            if (sheets.isEmpty()) {
                AppText(
                    "Aucune fiche rattachable pour le moment.",
                    color = AppTheme.colors.muted,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
                ) {
                    items(sheets, key = { it.id }) { sheet ->
                        AppText(
                            text = sheet.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(sheet.id) }
                                .padding(AppTheme.dimens.sm),
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(label = "Fermer", onClick = onDismiss, variant = ButtonVariant.Ghost)
        },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    )
}
