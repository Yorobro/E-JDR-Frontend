package eu.ejdr.presentation.features.settings

import androidx.lifecycle.ViewModel
import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel de l'écran des paramètres.
 *
 * Détient le thème courant ([currentTheme]) initialisé depuis [GetThemeUseCase] et
 * persiste chaque changement via [SetThemeUseCase]. Le ViewModel étant retenu par la
 * destination, le thème affiché survit à la recomposition.
 *
 * @property getTheme Use case de lecture du thème persisté.
 * @property setTheme Use case de persistance du thème.
 */
class SettingsViewModel(
    getTheme: GetThemeUseCase,
    private val setTheme: SetThemeUseCase,
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(getTheme())
    val currentTheme: StateFlow<ThemeVariant> = _currentTheme.asStateFlow()

    /**
     * Applique et **persiste** le thème choisi, puis met à jour l'état observé.
     *
     * @param theme Nouveau thème sélectionné.
     */
    fun onThemeSelected(theme: ThemeVariant) {
        setTheme(theme)
        _currentTheme.value = theme
    }
}
