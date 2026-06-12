package eu.ejdr.application.features.settings.abstraction.usecase

import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.error.SettingsError

fun interface SetThemeUseCase {
    /**
     * Persiste le thème choisi.
     *
     * @return [Result.Success] si la persistance a réussi, sinon
     * [Result.Failure] avec [SettingsError.ThemePersistenceFailed].
     */
    operator fun invoke(theme: ThemeVariant): Result<Unit, SettingsError>
}
