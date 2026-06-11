package eu.ejdr.application.features.auth.abstraction.service

/**
 * Port de persistance de session : abstraction des opérations de stockage local
 * liées au refresh_token.
 *
 * Implémenté par la couche infrastructure ([eu.ejdr.infrastructure.security.SecureCookiesStorage]).
 * Sépare la préoccupation de persistance du reste du repository HTTP, ce qui permet
 * de tester chaque partie indépendamment.
 */
interface SessionPersistence {
    /** Renvoie `true` si un refresh_token est persisté localement. */
    fun hasPersistedSession(): Boolean

    /** Supprime le refresh_token persisté (déconnexion ou session invalide). */
    fun clearPersisted()
}
