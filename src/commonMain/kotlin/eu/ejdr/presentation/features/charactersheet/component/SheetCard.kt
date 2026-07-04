package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.organism.AppCard
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Carré de délimitation d'une section de fiche (composant bête).
 *
 * Carte à fond `surface`, bordure, coins arrondis et ombre subtile, avec un titre en haut.
 * Reproduit les cadres de la fiche papier. Non cliquable.
 *
 * @param title Titre de la section.
 * @param modifier Modifier Compose appliqué à la carte.
 * @param content Contenu de la section.
 */
@Composable
fun SheetCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppCard(
        modifier = modifier,
        onClick = null,
        elevation = AppTheme.dimens.elevationSm,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md)) {
            AppText(text = title, style = AppTextStyle.Subtitle)
            content()
        }
    }
}
