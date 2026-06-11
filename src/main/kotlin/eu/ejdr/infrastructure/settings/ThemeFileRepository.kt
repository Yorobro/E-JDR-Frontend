package eu.ejdr.infrastructure.settings

import eu.ejdr.application.features.settings.abstraction.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import java.io.File
import java.util.Properties

class ThemeFileRepository(dataDir: File) : ThemeRepository {

    private val file = File(dataDir, "settings.properties")

    override fun getTheme(): ThemeVariant {
        if (!file.exists()) return ThemeVariant.LIGHT
        return runCatching {
            Properties().apply { file.inputStream().use { load(it) } }
                .getProperty("theme")
                ?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() }
                ?: ThemeVariant.LIGHT
        }.getOrDefault(ThemeVariant.LIGHT)
    }

    override fun setTheme(theme: ThemeVariant) {
        runCatching {
            val props = Properties()
            if (file.exists()) file.inputStream().use { props.load(it) }
            props.setProperty("theme", theme.name)
            file.outputStream().use { props.store(it, null) }
        }
    }
}
