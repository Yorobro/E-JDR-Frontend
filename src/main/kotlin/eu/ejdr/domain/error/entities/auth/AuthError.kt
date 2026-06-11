package eu.ejdr.domain.error.entities.auth

import eu.ejdr.domain.error.DomainError

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
     * Erreur non catégorisée, conservant le détail technique d'origine.
     *
     * @property detail Précision technique remontée pour le diagnostic.
     */
    data class Unknown(val detail: String) : AuthError("Erreur inattendue: $detail")
}
