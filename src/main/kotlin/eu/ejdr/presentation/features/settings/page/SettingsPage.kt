package eu.ejdr.presentation.features.settings.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.presentation.features.settings.SettingsViewModel
import eu.ejdr.presentation.features.settings.component.SettingsForm
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Page des paramètres (composant INTELLIGENT).
 *
 * Crée un [SettingsViewModel] retenu par la destination (qui lit/persiste le thème via
 * les use cases) et observe son état. Chaque changement est **persisté par le VM** puis
 * remonté à [eu.ejdr.presentation.App] via [onThemeChange] pour recomposer le design
 * system global. Le rendu est délégué au composant bête [SettingsForm].
 *
 * @param onThemeChange Callback appelé à chaque changement de thème, portant la nouvelle valeur.
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun SettingsPage(
    onThemeChange: (ThemeVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val getTheme = koinInject<GetThemeUseCase>()
    val setTheme = koinInject<SetThemeUseCase>()
    val viewModel = viewModel { SettingsViewModel(getTheme, setTheme) }
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
    ) {
        SettingsForm(
            currentTheme = currentTheme,
            onThemeChange = { newTheme ->
                viewModel.onThemeSelected(newTheme)
                onThemeChange(newTheme)
            },
        )
    }
}
