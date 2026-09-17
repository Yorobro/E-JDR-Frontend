package eu.ejdr.application.features.session.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.entities.SessionInvitation
import eu.ejdr.domain.features.session.entities.SessionLobby
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
     * Ouvre le lobby d'une session (réservé au MJ côté backend) en conviant les joueurs choisis.
     *
     * Le MJ n'est pas dans [participantUserIds] : il accède à la session via son rôle.
     *
     * @param sessionId identifiant de la session à passer en lobby.
     * @param participantUserIds identifiants des joueurs conviés.
     * @return le lobby créé, ou une [SessionError] ([SessionError.EmptyParticipantSelection] /
     *         [SessionError.ParticipantNotInGroup] / [SessionError.SessionNotLaunchable] / …).
     */
    suspend fun createLobby(
        sessionId: String,
        participantUserIds: List<String>,
    ): Result<SessionLobby, SessionError>

    /**
     * Convie des joueurs à un lobby **déjà ouvert** (réservé au MJ côté backend).
     *
     * Complète [createLobby], qui n'invite qu'à l'ouverture du salon : ici la session est déjà
     * en `LOBBY`. Sert au joueur oublié comme au refus accidentel — un joueur ayant refusé est
     * repassé en attente de réponse. Les joueurs déjà conviés sont ignorés côté serveur.
     *
     * @param sessionId identifiant de la session dont le lobby est ouvert.
     * @param participantUserIds identifiants des joueurs à convier.
     * @return le lobby complet à jour, ou une [SessionError] ([SessionError.LobbyNotOpen] si le
     *         salon n'est plus ouvert).
     */
    suspend fun inviteToLobby(
        sessionId: String,
        participantUserIds: List<String>,
    ): Result<SessionLobby, SessionError>

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

    /**
     * Répond à une invitation de session (côté joueur convié).
     *
     * @param sessionId identifiant de la session.
     * @param accept `true` pour accepter, `false` pour refuser.
     * @return [Unit] en cas de succès, ou une [SessionError].
     */
    suspend fun respondToInvitation(sessionId: String, accept: Boolean): Result<Unit, SessionError>

    /**
     * Liste les invitations de session en attente du joueur courant.
     *
     * @return les invitations en attente, ou une [SessionError].
     */
    suspend fun listMyInvitations(): Result<List<SessionInvitation>, SessionError>

    /**
     * Charge le lobby d'une session (statut + participants).
     *
     * @param sessionId identifiant de la session.
     * @return le [SessionLobby], ou une [SessionError].
     */
    suspend fun getLobby(sessionId: String): Result<SessionLobby, SessionError>

    /**
     * Démarre réellement la session (transition `LOBBY → ACTIVE`, réservé au MJ côté backend).
     *
     * @param sessionId identifiant de la session à démarrer.
     * @return [Unit] en cas de succès, ou une [SessionError].
     */
    suspend fun start(sessionId: String): Result<Unit, SessionError>
}
