package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppCornerFlourish
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * En-tête de contenu d'un écran (sous la top bar) : titre, sous-titre contextuel optionnel,
 * et action principale optionnelle alignée à droite (qui remplace l'ancien FAB).
 *
 * Composant bête : n'affiche que ce qu'on lui donne et délègue l'action à son slot.
 *
 * @param title Titre de l'écran.
 * @param modifier Modifier Compose appliqué à l'en-tête.
 * @param subtitle Sous-titre contextuel optionnel (ex. compteur de la liste).
 * @param action Slot d'action principale optionnel, aligné à droite (ex. bouton de création).
 * @param flourish Affiche un fleuron ornemental sous l'en-tête (signature « grimoire » des hubs).
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    flourish: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = AppTheme.dimens.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs)) {
                AppText(text = title, style = AppTextStyle.Title)
                if (subtitle != null) {
                    AppText(
                        text = subtitle,
                        style = AppTextStyle.Caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            if (action != null) action()
        }
        if (flourish) {
            AppCornerFlourish(modifier = Modifier.padding(bottom = AppTheme.dimens.md))
        }
    }
}
