package eu.ejdr.presentation.features.charactersheet.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page détail d'une fiche de personnage (composant bête).
 *
 * Écran minimal : il n'affiche que le nom et l'identifiant de la fiche reçus dans la clé de
 * navigation (aucun ViewModel, aucun appel réseau). Sert de cible cliquable aux tuiles de fiches ;
 * à enrichir ultérieurement.
 *
 * @param id Identifiant de la fiche (affiché).
 * @param name Nom de la fiche (affiché en titre).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun CharacterSheetDetailPage(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = name, style = AppTextStyle.Title)
        AppText(
            text = "Identifiant : $id",
            style = AppTextStyle.Body,
            color = AppTheme.colors.textSecondary,
        )
    }
}
