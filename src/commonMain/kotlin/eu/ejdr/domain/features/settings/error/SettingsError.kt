package eu.ejdr.domain.features.settings.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature paramètres.
 *
 * `sealed class` propre à la feature (même contrat que [eu.ejdr.domain.features.auth.error.AuthError]) :
 * garantit un `when` exhaustif et reste une variante de [DomainError]. Chaque variante porte
 * un message utilisateur prêt à afficher.
 */
sealed class SettingsError(override val message: String) : DomainError {
    /** La persistance du thème a échoué (écriture disque impossible). */
    data object ThemePersistenceFailed :
        SettingsError("Impossible d'enregistrer le thème. Réessayez.")
}
