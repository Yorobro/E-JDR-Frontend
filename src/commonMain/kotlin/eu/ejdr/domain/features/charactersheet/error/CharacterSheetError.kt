package eu.ejdr.domain.features.charactersheet.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature fiches de personnage (et de leur liaison aux campagnes).
 *
 * `sealed class` propre à la feature : garantit un `when` exhaustif, tout en restant une
 * variante de [DomainError]. Chaque variante porte un message utilisateur prêt à afficher.
 */
sealed class CharacterSheetError(override val message: String) : DomainError {
    /** Le nom de fiche fourni est invalide (vide ou trop long). */
    data object InvalidName : CharacterSheetError("Le nom de la fiche est invalide.")

    /** La fiche (ou la campagne) ciblée n'existe pas. */
    data object NotFound : CharacterSheetError("Élément introuvable.")

    /** L'utilisateur n'est pas autorisé à agir sur cette fiche. */
    data object AccessDenied :
        CharacterSheetError("Vous n'êtes pas autorisé à effectuer cette action.")

    /** Le maître du jeu ne peut pas ajouter une de ses fiches à sa propre campagne. */
    data object GmCannotJoinOwnCampaign :
        CharacterSheetError("Le maître du jeu ne peut pas ajouter une de ses fiches à sa campagne.")

    /** La fiche est déjà rattachée à cette campagne. */
    data object AlreadyInCampaign :
        CharacterSheetError("Cette fiche est déjà rattachée à cette campagne.")

    /** On ne peut pas copier une fiche vers sa propre campagne (choisir une autre campagne). */
    data object SameCampaignCopy :
        CharacterSheetError("Choisissez une autre campagne : la fiche y est déjà.")

    /** Échec de communication avec le serveur. */
    data object Network : CharacterSheetError("Erreur réseau, vérifiez votre connexion.")

    /**
     * Erreur non catégorisée.
     *
     * @property detail Précision technique pour le log uniquement (non affichée).
     */
    data class Unknown(val detail: String) :
        CharacterSheetError("Une erreur inattendue s'est produite.")
}
