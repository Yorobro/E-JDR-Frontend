package eu.ejdr.application.features.session.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError

/**
 * Port d'accès aux sessions : abstraction des opérations distantes (API REST).
 *
 * Implémenté par la couche infrastructure (HTTP) ; consommé par les use cases sans dépendre
 * des détails techniques. Toutes les opérations renvoient un [Result] : aucune exception ne
 * doit remonter.
 */
interface SessionRepository {
    /**
     * Liste les sessions d'une campagne (réservé au MJ côté backend).
     *
     * @param campaignId identifiant de la campagne parente.
     * @return la liste des sessions, ou une [SessionError] en cas d'échec.
     */
    suspend fun listByCampaign(campaignId: String): Result<List<Session>, SessionError>

    /**
     * Crée une session dans une campagne (réservé au MJ côté backend).
     *
     * @param campaignId identifiant de la campagne parente.
     * @param title titre de la session.
     * @param date date de la session au format `YYYY-MM-DD`.
     * @return la session créée, ou une [SessionError].
     */
    suspend fun create(
        campaignId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError>

    /**
     * Récupère le détail d'une session.
     *
     * @param sessionId identifiant de la session.
     * @return la session, ou une [SessionError] ([SessionError.NotFound] / [SessionError.AccessDenied]).
     */
    suspend fun get(sessionId: String): Result<Session, SessionError>

    /**
     * Met à jour le titre et la date d'une session.
     *
     * @param sessionId identifiant de la session.
     * @param title nouveau titre.
     * @param date nouvelle date au format `YYYY-MM-DD`.
     * @return la session mise à jour, ou une [SessionError].
     */
    suspend fun update(
        sessionId: String,
        title: String,
        date: String,
    ): Result<Session, SessionError>

    /**
     * Supprime une session.
     *
     * @param sessionId identifiant de la session à supprimer.
     * @return [Unit] si la suppression réussit, ou une [SessionError].
     */
    suspend fun delete(sessionId: String): Result<Unit, SessionError>
}
