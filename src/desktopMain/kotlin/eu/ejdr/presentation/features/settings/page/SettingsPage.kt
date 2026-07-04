package eu.ejdr.presentation.features.settings.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.presentation.features.settings.SettingsViewModel
import eu.ejdr.presentation.features.settings.component.SettingsForm
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

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
    val viewModel = koinViewModel {
        SettingsViewModel(get<GetThemeUseCase>(), get<SetThemeUseCase>())
    }
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
    ) {
        SettingsForm(
            currentTheme = currentTheme,
            // Ne propage le changement au design system global que si la persistance a réussi
            // (callback onApplied) : sinon l'application resterait sur un thème non enregistré.
            onThemeChange = { newTheme ->
                viewModel.onThemeSelected(newTheme, onApplied = onThemeChange)
            },
        )
        FormError(message = error)
    }
}
