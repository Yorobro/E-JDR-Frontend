package eu.ejdr.presentation.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran des paramètres.
 *
 * Charge le thème courant ([currentTheme]) de façon **asynchrone** via [GetThemeUseCase]
 * (l'I/O fichier ne bloque pas le thread UI) et persiste chaque changement via
 * [SetThemeUseCase]. Le ViewModel étant retenu par la destination, l'état survit à la
 * recomposition.
 *
 * La persistance peut **échouer** (disque indisponible) : dans ce cas l'état observé n'est
 * PAS modifié (pas de désynchronisation UI ↔ disque) et un message est exposé via [error].
 *
 * @param getTheme Use case de lecture du thème persisté.
 * @property setTheme Use case de persistance du thème.
 */
class SettingsViewModel(
    getTheme: GetThemeUseCase,
    private val setTheme: SetThemeUseCase,
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(ThemeVariant.DEFAULT)
    val currentTheme: StateFlow<ThemeVariant> = _currentTheme.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch { _currentTheme.value = getTheme() }
    }

    /**
     * Tente d'appliquer et de **persister** le thème choisi.
     *
     * Met à jour l'état observé **uniquement si la persistance réussit**. [onApplied] est
     * invoqué avec le thème en cas de succès (pour propager au design system global).
     *
     * @param theme Nouveau thème sélectionné.
     * @param onApplied Callback succès (thème persisté).
     */
    fun onThemeSelected(theme: ThemeVariant, onApplied: (ThemeVariant) -> Unit) {
        viewModelScope.launch {
            setTheme(theme).fold(
                onSuccess = {
                    _error.value = null
                    _currentTheme.value = theme
                    onApplied(theme)
                },
                onFailure = { settingsError -> _error.value = settingsError.message },
            )
        }
    }
}
