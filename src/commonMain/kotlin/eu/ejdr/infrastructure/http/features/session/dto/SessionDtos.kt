package eu.ejdr.infrastructure.http.features.session.dto

import kotlinx.serialization.Serializable

/**
 * Représentation JSON d'une session renvoyée par l'API.
 *
 * Contrat de transport HTTP, traduit vers l'entité domaine
 * [eu.ejdr.domain.features.session.entities.Session] par le mapper.
 *
 * @property id Identifiant unique de la session.
 * @property campaignId Identifiant de la campagne parente.
 * @property title Titre de la session.
 * @property date Date de la session au format `YYYY-MM-DD`.
 * @property status Statut courant (`PLANNED`, `LOBBY`, `ACTIVE`, `ENDED`).
 * @property createdAt Date de création au format ISO 8601.
 */
@Serializable
data class SessionDto(
    val id: String,
    val campaignId: String,
    val title: String,
    val date: String,
    val status: String = "PLANNED",
    val createdAt: String,
)

/**
 * Corps de requête de création de session (`POST /campaigns/{id}/sessions`).
 *
 * @property title Titre de la session.
 * @property date Date de la session au format `YYYY-MM-DD`.
 */
@Serializable
data class CreateSessionRequestDto(val title: String, val date: String)

/**
 * Corps de requête de mise à jour de session (`PUT /sessions/{id}`).
 *
 * @property title Nouveau titre.
 * @property date Nouvelle date au format `YYYY-MM-DD`.
 */
@Serializable
data class UpdateSessionRequestDto(val title: String, val date: String)

/**
 * Corps de réponse de `GET /campaigns/{id}/sessions` : l'API enveloppe la liste sous `sessions`.
 *
 * @property sessions Sessions de la campagne.
 */
@Serializable
data class SessionListResponseDto(val sessions: List<SessionDto>)

/**
 * Corps de requête d'ouverture du lobby (`POST /sessions/{id}/launch`).
 *
 * Le MJ ne figure pas dans [participantUserIds] : il accède à la session via son rôle, sans
 * s'inviter lui-même. La liste ne contient donc que les joueurs cochés.
 *
 * @property participantUserIds Identifiants des joueurs conviés à la session.
 */
@Serializable
data class CreateLobbyRequestDto(val participantUserIds: List<String>)

/**
 * Participant tel que renvoyé dans la réponse d'ouverture du lobby.
 *
 * @property userId Identifiant de l'utilisateur convié.
 * @property status État de l'invitation (`INVITED`, `ACCEPTED`, `REFUSED`).
 * @property characterSheetId Fiche choisie à l'acceptation, ou `null` s'il n'a pas encore répondu.
 */
@Serializable
data class LobbyParticipantDto(
    val userId: String,
    val status: String,
    val characterSheetId: String? = null,
)

/**
 * Corps de réponse de `POST /sessions/{id}/launch` : la session passée en lobby + ses invitations.
 *
 * @property sessionId Identifiant de la session passée en statut `LOBBY`.
 * @property status Statut de la session après ouverture (normalement `LOBBY`).
 * @property participants Joueurs conviés et leur état d'invitation.
 */
@Serializable
data class CreateLobbyResponseDto(
    val sessionId: String,
    val status: String,
    val participants: List<LobbyParticipantDto>,
)
