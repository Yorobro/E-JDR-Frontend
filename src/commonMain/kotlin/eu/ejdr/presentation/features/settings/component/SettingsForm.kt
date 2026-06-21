package eu.ejdr.presentation.features.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

@Composable
fun SettingsForm(
    currentTheme: ThemeVariant,
    onThemeChange: (ThemeVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = "Thème", style = AppTextStyle.Subtitle)
        // TODO(Task 5): remplacer ce sélecteur 2 boutons temporaire par le sélecteur 3 thèmes
        // (PARCHEMIN / TAUPE / GRIMOIRE). PARCHEMIN remplace provisoirement LIGHT, GRIMOIRE remplace DARK.
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md)) {
            AppButton(
                label = "Clair",
                onClick = { onThemeChange(ThemeVariant.PARCHEMIN) },
                variant = if (currentTheme == ThemeVariant.PARCHEMIN) ButtonVariant.Primary else ButtonVariant.Secondary,
            )
            AppButton(
                label = "Sombre",
                onClick = { onThemeChange(ThemeVariant.GRIMOIRE) },
                variant = if (currentTheme == ThemeVariant.GRIMOIRE) ButtonVariant.Primary else ButtonVariant.Secondary,
            )
        }
    }
}
