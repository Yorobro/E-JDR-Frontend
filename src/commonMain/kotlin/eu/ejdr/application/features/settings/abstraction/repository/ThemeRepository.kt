package eu.ejdr.application.features.settings.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError

interface ThemeRepository {
    /** Lit le thème persisté, avec un repli sûr si rien n'est enregistré ou en cas d'erreur. */
    suspend fun getTheme(): ThemeVariant

    /**
     * Persiste le thème choisi.
     *
     * @return [Result.Success] si l'écriture a réussi, ou [SettingsError.ThemePersistenceFailed].
     */
    suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError>
}
