package eu.ejdr.domain.features.session.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature sessions.
 *
 * `sealed class` propre à la feature : garantit un `when` exhaustif côté use cases et
 * présentation, tout en restant une variante de [DomainError]. Chaque variante porte un
 * message utilisateur prêt à afficher.
 */
sealed class SessionError(override val message: String) : DomainError {
    /** Le titre fourni est invalide (vide ou trop long). */
    data object InvalidTitle : SessionError("Le titre de la session est invalide.")

    /** La date fournie est invalide (format attendu : AAAA-MM-JJ). */
    data object InvalidDate : SessionError("La date de la session est invalide.")

    /** La session ciblée n'existe pas (ou plus). */
    data object NotFound : SessionError("Session introuvable.")

    /** L'utilisateur n'est pas autorisé à gérer les sessions de cette campagne. */
    data object AccessDenied :
        SessionError("Vous n'êtes pas autorisé à gérer les sessions de cette campagne.")

    /** Échec de communication avec le serveur (connectivité, timeout). */
    data object Network : SessionError("Erreur réseau, vérifiez votre connexion.")

    /**
     * Erreur non catégorisée.
     *
     * Le [message] affiché est **générique** ; le [detail] technique n'est jamais montré
     * à l'utilisateur (conservé pour le diagnostic uniquement).
     *
     * @property detail Précision technique pour le log uniquement (non affichée).
     */
    data class Unknown(val detail: String) : SessionError("Une erreur inattendue s'est produite.")
}
