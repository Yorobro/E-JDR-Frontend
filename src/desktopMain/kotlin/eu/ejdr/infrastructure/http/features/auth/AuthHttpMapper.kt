package eu.ejdr.infrastructure.http.features.auth

import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import eu.ejdr.infrastructure.http.features.auth.dto.AuthResponseDto
import io.ktor.http.HttpStatusCode

/**
 * Traduit les contrats de transport HTTP (DTO + statut HTTP) vers le domaine.
 *
 * C'est la frontière qui isole la couche application des détails du protocole :
 * les réponses brutes deviennent des entités ([User]) et les échecs deviennent
 * des erreurs métier ([AuthError]). Sans état, toutes les opérations sont pures.
 */
object AuthHttpMapper {

    /**
     * Convertit la réponse d'authentification reçue en entité domaine.
     *
     * @param dto Réponse JSON désérialisée renvoyée par le serveur.
     * @return L'[User] correspondant.
     */
    fun toUser(dto: AuthResponseDto): User = User(id = dto.userId, email = dto.email, pseudo = dto.pseudo)

    /**
     * Traduit un échec HTTP en erreur métier d'authentification.
     *
     * Le **code applicatif** prime quand il est présent (contrat partagé avec le backend,
     * cf. les codes émis par `AuthHttpMapper.toHttpStatus` côté serveur) car un même statut
     * 401 recouvre deux cas distincts : `INVALID_CREDENTIALS` (login) et
     * `INVALID_REFRESH_TOKEN` (session à renouveler). À défaut de code, on retombe sur le
     * statut HTTP, puis sur [AuthError.Unknown].
     *
     * @param status Statut HTTP retourné par l'API.
     * @param code Code d'erreur applicatif éventuel (issu d'[eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto]).
     * @param message Message d'erreur lisible éventuel (issu d'[eu.ejdr.infrastructure.http.features.auth.dto.ApiErrorDto]).
     * @return L'[AuthError] du domaine correspondant.
     */
    fun toAuthError(status: HttpStatusCode, code: String?, message: String?): AuthError =
        when (code) {
            "INVALID_CREDENTIALS" -> AuthError.InvalidCredentials
            "EMAIL_ALREADY_USED" -> AuthError.EmailAlreadyUsed
            "INVALID_REFRESH_TOKEN" -> AuthError.SessionExpired
            "ACCOUNT_LOCKED" -> AuthError.AccountLocked
            "INVALID_EMAIL" -> AuthError.InvalidEmail
            "WEAK_PASSWORD" -> AuthError.WeakPassword
            else -> when (status) {
                HttpStatusCode.Unauthorized -> AuthError.InvalidCredentials
                HttpStatusCode.Conflict -> AuthError.EmailAlreadyUsed
                else -> AuthError.Unknown(message ?: code ?: status.description)
            }
        }
}
