package eu.ejdr.infrastructure.http.features.campaign.dto

import kotlinx.serialization.Serializable

/**
 * Représentation JSON d'une campagne renvoyée par l'API.
 *
 * Contrat de transport HTTP, traduit vers l'entité domaine
 * [eu.ejdr.domain.features.campaign.entities.Campaign] par le mapper.
 *
 * @property id Identifiant unique de la campagne.
 * @property name Nom de la campagne.
 * @property gameMasterId Identifiant du maître du jeu (propriétaire de la campagne).
 * @property createdAt Date de création au format ISO 8601.
 */
@Serializable
data class CampaignDto(
    val id: String,
    val name: String,
    val gameMasterId: String,
    val createdAt: String,
)

/**
 * Corps de requête envoyé au serveur pour créer une campagne (`POST /campaigns`).
 *
 * @property name Nom de la campagne à créer.
 * @property groupId Identifiant du groupe actif auquel rattacher la campagne.
 */
@Serializable
data class CreateCampaignRequestDto(val name: String, val groupId: String)

/**
 * Corps de réponse de `GET /campaigns` : l'API enveloppe la liste sous la clé `campaigns`.
 *
 * @property campaigns Campagnes du maître du jeu courant.
 */
@Serializable
data class CampaignListResponseDto(val campaigns: List<CampaignDto>)
