package eu.ejdr.infrastructure.settings

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThemeFileRepository(dataDir: File) : ThemeRepository {

    private val file = File(dataDir, "settings.properties")

    override suspend fun getTheme(): ThemeVariant = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ThemeVariant.DEFAULT
        runCatching {
            Properties().apply { file.inputStream().use { load(it) } }
                .getProperty("theme")
                ?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() }
                ?: ThemeVariant.DEFAULT
        }.getOrDefault(ThemeVariant.DEFAULT)
    }

    override suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError> =
        withContext(Dispatchers.IO) {
            val written = runCatching {
                val props = Properties()
                if (file.exists()) file.inputStream().use { props.load(it) }
                props.setProperty("theme", theme.name)
                file.outputStream().use { props.store(it, null) }
            }.isSuccess
            if (written) Result.Success(Unit) else Result.Failure(SettingsError.ThemePersistenceFailed)
        }
}
