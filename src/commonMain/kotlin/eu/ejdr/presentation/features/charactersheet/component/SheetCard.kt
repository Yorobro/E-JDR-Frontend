package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
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
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = AppTheme.colors.surface,
        contentColor = AppTheme.colors.text,
        shadowElevation = AppTheme.dimens.elevationSm,
        border = BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimens.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        ) {
            AppText(text = title, style = AppTextStyle.Subtitle)
            content()
        }
    }
}
