package eu.ejdr.infrastructure.http.auth

import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import eu.ejdr.infrastructure.http.auth.dto.AuthResponseDto
import io.ktor.http.HttpStatusCode

/**
 * Traduit les contrats de transport HTTP (DTO + statut HTTP) vers le domaine.
 *
 * C'est la frontière qui isole la couche application des détails du protocole :
 * les réponses brutes deviennent des entités ([User]) et les échecs deviennent
 * des erreurs métier ([AuthError]).
 */
class AuthHttpMapper {

    /**
     * Convertit la réponse d'authentification reçue en entité domaine.
     *
     * @param dto Réponse JSON désérialisée renvoyée par le serveur.
     * @return L'[User] correspondant.
     */
    fun toUser(dto: AuthResponseDto): User = User(id = dto.userId, email = dto.email)

    /**
     * Traduit un échec HTTP en erreur métier d'authentification.
     *
     * Le statut HTTP prime pour identifier les cas connus (401 -> identifiants
     * invalides, 409 -> e-mail déjà utilisé, 403 -> session expirée) ; les
     * champs {code, message} du corps d'erreur ne servent qu'à enrichir le cas
     * [AuthError.Unknown] par défaut.
     *
     * @param status Statut HTTP retourné par l'API.
     * @param code Code d'erreur applicatif éventuel (issu d'[eu.ejdr.infrastructure.http.auth.dto.ApiErrorDto]).
     * @param message Message d'erreur lisible éventuel (issu d'[eu.ejdr.infrastructure.http.auth.dto.ApiErrorDto]).
     * @return L'[AuthError] du domaine correspondant.
     */
    fun toAuthError(status: HttpStatusCode, code: String?, message: String?): AuthError =
        when (status) {
            HttpStatusCode.Unauthorized -> AuthError.InvalidCredentials
            HttpStatusCode.Conflict -> AuthError.EmailAlreadyUsed
            HttpStatusCode.Forbidden -> AuthError.SessionExpired
            else -> AuthError.Unknown(message ?: code ?: status.description)
        }
}
