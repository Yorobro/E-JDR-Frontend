package eu.ejdr.application.auth.abstraction.repository

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Port d'accès à l'authentification : abstraction des opérations distantes et locales
 * liées à la session utilisateur.
 *
 * Implémenté par la couche infrastructure ; consommé par les use cases et services
 * de la couche application sans dépendre des détails techniques (HTTP, stockage, etc.).
 * Toutes les opérations renvoient un [Result] : aucune exception ne doit remonter.
 */
interface AuthRepository {
    /**
     * Authentifie un utilisateur existant.
     *
     * @param credentials identifiants de connexion à vérifier
     * @return l'[User] authentifié, ou une [AuthError] en cas d'échec
     */
    suspend fun login(credentials: Credentials): Result<User, AuthError>

    /**
     * Crée un nouveau compte utilisateur.
     *
     * @param credentials identifiants du compte à créer
     * @return l'[User] nouvellement enregistré, ou une [AuthError] en cas d'échec
     */
    suspend fun register(credentials: Credentials): Result<User, AuthError>

    /**
     * Rafraîchit la session courante à partir des informations persistées
     * (renouvellement de jeton).
     *
     * @return l'[User] dont la session est renouvelée, ou une [AuthError] sinon
     */
    suspend fun refresh(): Result<User, AuthError>

    /**
     * Termine la session courante et invalide les informations persistées.
     *
     * @return [Unit] si la déconnexion réussit, ou une [AuthError] sinon
     */
    suspend fun logout(): Result<Unit, AuthError>

    /**
     * Récupère le profil de l'utilisateur courant auprès du serveur (`GET /me`).
     *
     * Première route protégée : un 401 éventuel est d'abord traité par l'intercepteur
     * de rafraîchissement silencieux du client HTTP ; s'il parvient jusqu'ici, la
     * session est réellement expirée.
     *
     * @return l'[User] courant, ou une [AuthError] ([AuthError.SessionExpired] si la
     * session n'est plus valide)
     */
    suspend fun me(): Result<User, AuthError>

    /**
     * Indique si une session est persistée localement (sans la valider auprès du serveur).
     *
     * @return `true` si des informations de session existent localement, `false` sinon
     */
    fun hasPersistedSession(): Boolean
}
