package eu.ejdr.infrastructure.http.features.charactersheet.dto

import kotlinx.serialization.Serializable

/**
 * Représentation JSON d'une fiche renvoyée par l'API.
 *
 * @property id Identifiant de la fiche.
 * @property ownerId Identifiant du propriétaire.
 * @property name Nom de la fiche.
 * @property createdAt Date de création au format ISO 8601.
 */
@Serializable
data class CharacterSheetDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val createdAt: String,
)

/** Corps de requête de création d'une fiche (`POST /character-sheets`). */
@Serializable
data class CreateCharacterSheetRequestDto(val name: String)

/** Réponse de `GET /character-sheets` (l'API enveloppe la liste). */
@Serializable
data class CharacterSheetListResponseDto(val characterSheets: List<CharacterSheetDto>)

/** Réponse de `GET /campaigns/:id/characters` (fiches rattachées). */
@Serializable
data class CampaignCharactersResponseDto(val characters: List<CharacterSheetDto>)

/** Corps de requête de rattachement (`POST /campaigns/:id/characters`). */
@Serializable
data class LinkCharacterRequestDto(val characterSheetId: String)
