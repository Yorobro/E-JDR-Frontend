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

    /** Aucun joueur sélectionné pour rejoindre le lobby. */
    data object EmptyParticipantSelection : SessionError("Veuillez sélectionner au moins un joueur.")

    /** Au moins un joueur sélectionné n'est pas membre du groupe. */
    data object ParticipantNotInGroup : SessionError("Un ou plusieurs joueurs ne font pas partie du groupe.")

    /** La session n'est pas dans un état permettant d'ouvrir un lobby (déjà lancée, etc.). */
    data object SessionNotLaunchable : SessionError("Cette session ne peut pas être lancée.")

    /** Le salon d'attente n'est pas (ou plus) ouvert : impossible d'y convier un joueur. */
    data object LobbyNotOpen : SessionError("Le salon d'attente n'est plus ouvert.")

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
