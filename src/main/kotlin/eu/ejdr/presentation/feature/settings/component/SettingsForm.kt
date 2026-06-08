package eu.ejdr.presentation.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.application.settings.abstraction.ThemeVariant
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
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md)) {
            AppButton(
                label = "Clair",
                onClick = { onThemeChange(ThemeVariant.LIGHT) },
                variant = if (currentTheme == ThemeVariant.LIGHT) ButtonVariant.Primary else ButtonVariant.Secondary,
            )
            AppButton(
                label = "Sombre",
                onClick = { onThemeChange(ThemeVariant.DARK) },
                variant = if (currentTheme == ThemeVariant.DARK) ButtonVariant.Primary else ButtonVariant.Secondary,
            )
        }
    }
}
