package eu.ejdr.presentation.features.settings.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.presentation.features.settings.component.SettingsForm
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Page des paramètres (composant INTELLIGENT).
 *
 * Seul endroit de la feature settings qui injecte [GetThemeUseCase] et [SetThemeUseCase].
 * Elle détient l'état local du thème affiché, persiste le choix via le use case, et remonte
 * le changement à [App] via [onThemeChange] pour que [AppTheme] recompose avec les bonnes couleurs.
 * Le rendu est délégué au composant bête [SettingsForm].
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
    var currentTheme by remember { mutableStateOf(getTheme()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
    ) {
        SettingsForm(
            currentTheme = currentTheme,
            onThemeChange = { newTheme ->
                currentTheme = newTheme
                setTheme(newTheme)
                onThemeChange(newTheme)
            },
        )
    }
}
