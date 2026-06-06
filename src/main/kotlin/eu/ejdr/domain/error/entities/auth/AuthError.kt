package eu.ejdr.domain.error.entities.auth

import eu.ejdr.domain.error.DomainError

sealed class AuthError(override val message: String) : DomainError {
    data object InvalidCredentials : AuthError("Identifiants invalides.")
    data object EmailAlreadyUsed : AuthError("Cet email est déjà utilisé.")
    data object SessionExpired : AuthError("Session expirée, veuillez vous reconnecter.")
    data object NoPersistedSession : AuthError("Aucune session enregistrée.")
    data object Network : AuthError("Erreur réseau, vérifiez votre connexion.")
    data class Unknown(val detail: String) : AuthError("Erreur inattendue: $detail")
}
