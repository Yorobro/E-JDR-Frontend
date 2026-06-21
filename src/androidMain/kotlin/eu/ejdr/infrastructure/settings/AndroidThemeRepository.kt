package eu.ejdr.infrastructure.settings

import android.content.Context
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Implémentation Android de [ThemeRepository], adossée aux SharedPreferences classiques. */
class AndroidThemeRepository(context: Context) : ThemeRepository {

    private val prefs = context.getSharedPreferences("ejdr_settings", Context.MODE_PRIVATE)
    private val themeKey = "theme"

    override suspend fun getTheme(): ThemeVariant = withContext(Dispatchers.IO) {
        prefs.getString(themeKey, null)
            ?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() }
            ?: ThemeVariant.DEFAULT
    }

    override suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError> =
        withContext(Dispatchers.IO) {
            val written = runCatching {
                prefs.edit().putString(themeKey, theme.name).commit()
            }.getOrDefault(false)
            if (written) Result.Success(Unit) else Result.Failure(SettingsError.ThemePersistenceFailed)
        }
}
