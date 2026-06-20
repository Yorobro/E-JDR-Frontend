package eu.ejdr.domain.features.reference.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature éléments de référence (catalogue + liaison aux fiches).
 *
 * `sealed class` propre à la feature : garantit un `when` exhaustif côté use cases et présentation,
 * tout en restant une variante de [DomainError]. Chaque variante porte un message prêt à afficher.
 */
sealed class ReferenceError(override val message: String) : DomainError {
    /** Le nom fourni est invalide (vide ou trop long). */
    data object InvalidName : ReferenceError("Le nom de l'élément est invalide.")

    /** Un élément du même nom existe déjà dans cette catégorie. */
    data object NameAlreadyUsed : ReferenceError("Un élément porte déjà ce nom.")

    /** L'élément ciblé n'existe pas (ou plus). */
    data object NotFound : ReferenceError("Élément introuvable.")

    /** L'utilisateur n'est pas autorisé à agir sur cette ressource. */
    data object AccessDenied : ReferenceError("Vous n'êtes pas autorisé à effectuer cette action.")

    /** Échec de communication avec le serveur (connectivité, timeout). */
    data object Network : ReferenceError("Erreur réseau, vérifiez votre connexion.")

    /**
     * Erreur non catégorisée.
     *
     * Le [message] affiché est **générique** ; le [detail] technique n'est jamais montré à
     * l'utilisateur (conservé pour le diagnostic uniquement).
     *
     * @property detail Précision technique pour le log uniquement (non affichée).
     */
    data class Unknown(val detail: String) : ReferenceError("Une erreur inattendue s'est produite.")
}
