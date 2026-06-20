package eu.ejdr.domain.features.update.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature mise à jour.
 *
 * `sealed class` propre à la feature (même contrat que les autres erreurs de domaine) :
 * garantit un `when` exhaustif et reste une variante de [DomainError]. Chaque variante
 * porte un message utilisateur prêt à afficher.
 */
sealed class UpdateError(override val message: String) : DomainError {
    /** La vérification de mise à jour a échoué (réseau, serveur indisponible). */
    data object CheckFailed :
        UpdateError("Impossible de vérifier les mises à jour. Réessayez plus tard.")

    /** Le téléchargement ou le lancement de l'installeur a échoué. */
    data object DownloadFailed :
        UpdateError("Le téléchargement de la mise à jour a échoué. Réessayez.")

    /**
     * L'intégrité de l'installeur téléchargé n'a pas pu être garantie : empreinte SHA-256
     * absente de la release, URL hors d'un hôte GitHub de confiance, ou empreinte calculée
     * différente de l'empreinte publiée. Le lancement est refusé pour ne jamais exécuter un
     * binaire non vérifié.
     */
    data object IntegrityCheckFailed :
        UpdateError("La mise à jour n'a pas pu être vérifiée et a été bloquée par sécurité.")
}
