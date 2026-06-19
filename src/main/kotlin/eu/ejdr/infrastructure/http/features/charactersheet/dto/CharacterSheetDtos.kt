package eu.ejdr.infrastructure.http.features.charactersheet.dto

import kotlinx.serialization.Serializable

/** Bourse JSON (pièces brutes). */
@Serializable
data class PurseDto(val gold: Int = 0, val silver: Int = 0, val copper: Int = 0)

/**
 * Représentation JSON d'une fiche renvoyée par l'API.
 *
 * Les champs détaillés ont un **défaut `null`** : c'est ce qui permet à la réponse **liste**
 * (projection nom seul, sans ces clés) de désérialiser dans ce DTO élargi. Seul le détail
 * (`GET /character-sheets/:id`) les renseigne.
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
    val formationId: String? = null,
    val niveau: Int? = null,
    val peupleId: String? = null,
    val sexe: String? = null,
    val tailleEtPoids: String? = null,
    val age: Int? = null,
    val apparence: String? = null,
    val dexterite: Int? = null,
    val intelligence: Int? = null,
    val perception: Int? = null,
    val social: Int? = null,
    val vigueur: Int? = null,
    val pointsDeVie: Int? = null,
    val pointsDeMagie: Int? = null,
    val protection: Int? = null,
    val purse: PurseDto? = null,
    val sortsEtMiracles: String? = null,
    val notes: String? = null,
)

/** Corps de requête de création d'une fiche (`POST /character-sheets`). */
@Serializable
data class CreateCharacterSheetRequestDto(val name: String, val groupId: String)

/**
 * Corps de requête de mise à jour d'une fiche (`PUT /character-sheets/:id`).
 *
 * Le `name` est requis ; les champs détaillés sont optionnels (omis ou `null`). Le serveur
 * normalise (trim, bornage, entiers ≥ 0).
 */
@Serializable
data class UpdateCharacterSheetRequestDto(
    val name: String,
    val formationId: String? = null,
    val niveau: Int? = null,
    val peupleId: String? = null,
    val sexe: String? = null,
    val tailleEtPoids: String? = null,
    val age: Int? = null,
    val apparence: String? = null,
    val dexterite: Int? = null,
    val intelligence: Int? = null,
    val perception: Int? = null,
    val social: Int? = null,
    val vigueur: Int? = null,
    val pointsDeVie: Int? = null,
    val pointsDeMagie: Int? = null,
    val protection: Int? = null,
    val purse: PurseDto? = null,
    val sortsEtMiracles: String? = null,
    val notes: String? = null,
)

/** Réponse de `GET /character-sheets` (l'API enveloppe la liste). */
@Serializable
data class CharacterSheetListResponseDto(val characterSheets: List<CharacterSheetDto>)

/** Réponse de `GET /campaigns/:id/characters` (fiches rattachées). */
@Serializable
data class CampaignCharactersResponseDto(val characters: List<CharacterSheetDto>)

/** Corps de requête de rattachement (`POST /campaigns/:id/characters`). */
@Serializable
data class LinkCharacterRequestDto(val characterSheetId: String)

/** Une campagne rattachée à une fiche, avec le pseudo du MJ (`GET /character-sheets/:id/campaigns`). */
@Serializable
data class SheetCampaignDto(
    val campaignId: String,
    val campaignName: String,
    val gameMasterPseudo: String,
)

/** Réponse de `GET /character-sheets/:id/campaigns`. */
@Serializable
data class SheetCampaignsResponseDto(val campaigns: List<SheetCampaignDto>)
