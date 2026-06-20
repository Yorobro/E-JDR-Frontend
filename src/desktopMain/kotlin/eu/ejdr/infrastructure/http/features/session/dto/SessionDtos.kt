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
 * @property createdAt Date de création au format ISO 8601.
 */
@Serializable
data class SessionDto(
    val id: String,
    val campaignId: String,
    val title: String,
    val date: String,
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
