package eu.ejdr.presentation.features.settings.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.settings.SettingsViewModel
import eu.ejdr.presentation.features.settings.component.SettingsForm
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page des paramètres Android (composant intelligent).
 *
 * Crée un [SettingsViewModel] retenu par la destination et observe son état. Chaque changement
 * de thème est persisté par le VM puis remonté à App via [onThemeChange] pour recomposer le
 * design system. Rendu délégué au composant commun [SettingsForm].
 */
@Composable
fun SettingsPage(
    onThemeChange: (ThemeVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel {
        SettingsViewModel(get<GetThemeUseCase>(), get<SetThemeUseCase>())
    }
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.md),
    ) {
        AppText(text = "Paramètres", style = AppTextStyle.Title)
        SettingsForm(
            currentTheme = currentTheme,
            onThemeChange = { newTheme ->
                viewModel.onThemeSelected(newTheme, onApplied = onThemeChange)
            },
        )
        FormError(message = error)
    }
}
