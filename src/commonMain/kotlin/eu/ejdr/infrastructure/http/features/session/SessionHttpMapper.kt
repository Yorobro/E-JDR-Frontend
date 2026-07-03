package eu.ejdr.infrastructure.http.features.session

import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.domain.features.session.error.SessionError
import eu.ejdr.infrastructure.http.features.session.dto.CreateLobbyResponseDto
import eu.ejdr.infrastructure.http.features.session.dto.SessionDto
import io.ktor.http.HttpStatusCode

/**
 * Traduit les contrats de transport HTTP (DTO + statut HTTP) de la feature sessions vers le
 * domaine. Frontière isolant la couche application des détails du protocole. Sans état :
 * toutes les opérations sont pures.
 */
object SessionHttpMapper {

    /**
     * Convertit une session reçue de l'API en entité domaine.
     *
     * @param dto Session JSON désérialisée.
     * @return La [Session] correspondante.
     */
    fun toSession(dto: SessionDto): Session =
        Session(
            id = dto.id,
            campaignId = dto.campaignId,
            title = dto.title,
            date = dto.date,
            status = dto.status,
            createdAt = dto.createdAt,
        )

    /**
     * Convertit la réponse d'ouverture du lobby en entité domaine.
     *
     * @param dto Réponse JSON désérialisée de `POST /sessions/{id}/launch`.
     * @return Le [SessionLobby] correspondant.
     */
    fun toLobby(dto: CreateLobbyResponseDto): SessionLobby =
        SessionLobby(
            sessionId = dto.sessionId,
            status = dto.status,
            participants = dto.participants.map { p ->
                LobbyParticipant(
                    userId = p.userId,
                    status = p.status,
                    characterSheetId = p.characterSheetId,
                )
            },
        )

    /**
     * Traduit un échec HTTP en erreur métier session.
     *
     * Le **code applicatif** prime quand il est présent (contrat partagé avec le backend) ;
     * à défaut on retombe sur le statut HTTP, puis sur [SessionError.Unknown].
     *
     * @param status Statut HTTP retourné par l'API.
     * @param code Code d'erreur applicatif éventuel.
     * @param message Message d'erreur lisible éventuel.
     * @return La [SessionError] du domaine correspondante.
     */
    fun toError(status: HttpStatusCode, code: String?, message: String?): SessionError =
        when (code) {
            "INVALID_SESSION_TITLE" -> SessionError.InvalidTitle
            "INVALID_SESSION_DATE" -> SessionError.InvalidDate
            "SESSION_NOT_FOUND" -> SessionError.NotFound
            "CAMPAIGN_NOT_FOUND" -> SessionError.NotFound
            "CAMPAIGN_ACCESS_DENIED" -> SessionError.AccessDenied
            "NOT_GROUP_EDITOR", "NOT_GROUP_MEMBER" -> SessionError.AccessDenied
            "EMPTY_PARTICIPANT_SELECTION" -> SessionError.EmptyParticipantSelection
            "PARTICIPANT_NOT_IN_GROUP" -> SessionError.ParticipantNotInGroup
            "SESSION_NOT_LAUNCHABLE" -> SessionError.SessionNotLaunchable
            else -> when (status) {
                HttpStatusCode.NotFound -> SessionError.NotFound
                HttpStatusCode.Forbidden -> SessionError.AccessDenied
                HttpStatusCode.BadRequest -> SessionError.InvalidTitle
                else -> SessionError.Unknown(message ?: code ?: status.description)
            }
        }
}
