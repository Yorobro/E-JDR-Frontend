package eu.ejdr.application.features.settings.abstraction.repository

import eu.ejdr.application.features.settings.abstraction.ThemeVariant

interface ThemeRepository {
    /** Lit le thème persisté, avec un repli sûr si rien n'est enregistré ou en cas d'erreur. */
    fun getTheme(): ThemeVariant

    /**
     * Persiste le thème choisi.
     *
     * @return `true` si l'écriture a réussi, `false` si elle a échoué (disque indisponible…).
     */
    fun setTheme(theme: ThemeVariant): Boolean
}
