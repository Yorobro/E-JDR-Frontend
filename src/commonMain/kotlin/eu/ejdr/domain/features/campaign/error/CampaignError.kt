package eu.ejdr.domain.features.campaign.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature campagnes.
 *
 * `sealed class` propre à la feature : garantit un `when` exhaustif côté use cases et
 * présentation, tout en restant une variante de [DomainError]. Chaque variante porte un
 * message utilisateur prêt à afficher.
 */
sealed class CampaignError(override val message: String) : DomainError {
    /** Le nom de campagne fourni est invalide (vide ou trop long). */
    data object InvalidName : CampaignError("Le nom de la campagne est invalide.")

    /** La campagne ciblée n'existe pas (ou plus). */
    data object NotFound : CampaignError("Campagne introuvable.")

    /** L'utilisateur n'est pas autorisé à agir sur cette campagne. */
    data object AccessDenied : CampaignError("Vous n'êtes pas autorisé à modifier cette campagne.")

    /** Échec de communication avec le serveur (connectivité, timeout). */
    data object Network : CampaignError("Erreur réseau, vérifiez votre connexion.")

    /**
     * Erreur non catégorisée.
     *
     * Le [message] affiché est **générique** ; le [detail] technique n'est jamais montré
     * à l'utilisateur (conservé pour le diagnostic uniquement).
     *
     * @property detail Précision technique pour le log uniquement (non affichée).
     */
    data class Unknown(val detail: String) : CampaignError("Une erreur inattendue s'est produite.")
}
