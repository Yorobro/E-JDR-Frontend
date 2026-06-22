package eu.ejdr.presentation.features.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm)) {
            ThemeVariant.entries.forEach { variant ->
                AppButton(
                    label = themeLabel(variant),
                    onClick = { onThemeChange(variant) },
                    variant = if (currentTheme == variant) ButtonVariant.Primary else ButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun themeLabel(variant: ThemeVariant): String = when (variant) {
    ThemeVariant.PARCHEMIN -> "Parchemin — clair chaleureux"
    ThemeVariant.TAUPE -> "Taupe — clair minimaliste"
    ThemeVariant.GRIMOIRE -> "Grimoire — sombre"
}
