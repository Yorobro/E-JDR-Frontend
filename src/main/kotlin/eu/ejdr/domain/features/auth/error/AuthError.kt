package eu.ejdr.domain.features.auth.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature d'authentification.
 *
 * `sealed class` propre à la feature : garantit un `when` exhaustif côté use cases
 * et présentation, tout en restant une variante de [DomainError]. Chaque variante
 * porte un message utilisateur prêt à afficher.
 */
sealed class AuthError(override val message: String) : DomainError {
    /** Email ou mot de passe incorrect lors de la connexion. */
    data object InvalidCredentials : AuthError("Identifiants invalides.")

    /** Inscription refusée : l'email est déjà rattaché à un compte. */
    data object EmailAlreadyUsed : AuthError("Cet email est déjà utilisé.")

    /** La session courante n'est plus valide et doit être renouvelée. */
    data object SessionExpired : AuthError("Session expirée, veuillez vous reconnecter.")

    /** Aucune session persistée à restaurer (premier lancement, déconnexion). */
    data object NoPersistedSession : AuthError("Aucune session enregistrée.")

    /** Compte temporairement verrouillé après trop de tentatives. */
    data object AccountLocked : AuthError("Compte temporairement verrouillé. Réessayez dans quelques minutes.")

    /** Échec de communication avec le serveur (connectivité, timeout). */
    data object Network : AuthError("Erreur réseau, vérifiez votre connexion.")

    /**
     * Erreur non catégorisée.
     *
     * Le [message] affiché est **générique** : le [detail] technique (souvent du
     * texte serveur brut) n'est jamais montré à l'utilisateur — il est conservé
     * à part, uniquement pour le diagnostic/journalisation, afin de ne pas faire
     * fuiter de contenu serveur dans l'UI.
     *
     * @property detail Précision technique pour le log uniquement (non affichée).
     */
    data class Unknown(val detail: String) : AuthError("Une erreur inattendue s'est produite.")
}
