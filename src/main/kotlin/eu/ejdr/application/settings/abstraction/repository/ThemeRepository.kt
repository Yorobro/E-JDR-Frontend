package eu.ejdr.application.settings.abstraction.repository

import eu.ejdr.application.settings.abstraction.ThemeVariant

interface ThemeRepository {
    fun getTheme(): ThemeVariant
    fun setTheme(theme: ThemeVariant)
}
