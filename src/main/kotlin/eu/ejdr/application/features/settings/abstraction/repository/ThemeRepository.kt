package eu.ejdr.application.features.settings.abstraction.repository

import eu.ejdr.application.features.settings.abstraction.ThemeVariant

interface ThemeRepository {
    fun getTheme(): ThemeVariant
    fun setTheme(theme: ThemeVariant)
}
