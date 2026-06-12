package eu.ejdr.presentation.features.settings

import androidx.lifecycle.ViewModel
import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.fold
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
 * La persistance peut **échouer** (disque indisponible) : dans ce cas l'état observé
 * n'est PAS modifié (pas de désynchronisation UI ↔ disque) et un message d'erreur est
 * exposé via [error]. La sélection d'un thème efface l'erreur précédente.
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Tente d'appliquer et de **persister** le thème choisi.
     *
     * Met à jour l'état observé **uniquement si la persistance réussit**, afin que l'UI
     * ne diverge jamais de ce qui est réellement enregistré.
     *
     * @param theme Nouveau thème sélectionné.
     * @return `true` si le thème a été persisté (l'appelant peut alors propager le
     * changement au design system global), `false` en cas d'échec.
     */
    fun onThemeSelected(theme: ThemeVariant): Boolean =
        setTheme(theme).fold(
            onSuccess = {
                _error.value = null
                _currentTheme.value = theme
                true
            },
            onFailure = { settingsError ->
                _error.value = settingsError.message
                false
            },
        )
}
