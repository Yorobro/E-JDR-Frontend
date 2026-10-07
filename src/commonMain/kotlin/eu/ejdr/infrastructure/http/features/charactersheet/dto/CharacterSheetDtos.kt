package eu.ejdr.infrastructure.http.features.charactersheet.dto

import eu.ejdr.infrastructure.http.features.reference.dto.StatBonusDto
import kotlinx.serialization.Serializable

/** Bourse JSON (pièces brutes). */
@Serializable
data class PurseDto(val gold: Int = 0, val silver: Int = 0, val copper: Int = 0)

/** Compétence apportée par une formation (bloc dérivé du détail). */
@Serializable
data class ResolvedCompetenceDto(val id: String, val name: String)

/**
 * **Peuple** résolu renvoyé dans le détail d'une fiche. Champs tolérants (défaut vide) : le front
 * doit continuer de fonctionner contre un backend pas encore déployé — il affichera alors une fiche
 * sans ligne de bonus de peuple, mais avec des totaux justes (ils viennent du serveur).
 *
 * Contrairement à la formation (mono-bonus), un peuple porte 0..N bonus et n'expose donc pas
 * `stat`/`bonus`.
 *
 * @property statBonuses Bonus apportés par le peuple (au plus un par statistique).
 */
@Serializable
data class ResolvedReferenceDto(
    val id: String,
    val name: String,
    val statBonuses: List<StatBonusDto> = emptyList(),
)

/**
 * Formation résolue renvoyée dans le détail : comme [ResolvedReferenceDto] + compétences apportées.
 * Champs tolérants (défaut `null`/vide).
 */
@Serializable
data class ResolvedFormationDto(
    val id: String,
    val name: String,
    val stat: String? = null,
    val bonus: Int? = null,
    val competences: List<ResolvedCompetenceDto> = emptyList(),
)

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
 * @property dexteriteTotale Total de dextérité calculé serveur (base + bonus peuple/formation), ou `null`.
 * @property intelligenceTotale Total d'intelligence calculé serveur, ou `null`.
 * @property perceptionTotale Total de perception calculé serveur, ou `null`.
 * @property socialTotale Total social calculé serveur, ou `null`.
 * @property vigueurTotale Total de vigueur calculé serveur, ou `null`.
 */
@Serializable
data class CharacterSheetDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val createdAt: String,
    val campaignId: String? = null,
    val campaignName: String? = null,
    val linkStatus: String? = null,
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
    val notes: String? = null,
    val formation: ResolvedFormationDto? = null,
    val peuple: ResolvedReferenceDto? = null,
    val dexteriteTotale: Int? = null,
    val intelligenceTotale: Int? = null,
    val perceptionTotale: Int? = null,
    val socialTotale: Int? = null,
    val vigueurTotale: Int? = null,
)

/**
 * Corps de requête de création d'une fiche (`POST /character-sheets`).
 *
 * @property name Nom de la fiche.
 * @property groupId Groupe actif auquel rattacher la fiche.
 * @property campaignId Campagne (requise) à laquelle la fiche demande son rattachement (statut PENDING).
 */
@Serializable
data class CreateCharacterSheetRequestDto(
    val name: String,
    val groupId: String,
    val campaignId: String,
)

/** Corps de requête de copie d'une fiche vers une autre campagne (`POST /character-sheets/:id/copy`). */
@Serializable
data class CopyCharacterSheetRequestDto(val targetCampaignId: String)

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
    val notes: String? = null,
)

/** Réponse de `GET /character-sheets` (l'API enveloppe la liste). */
@Serializable
data class CharacterSheetListResponseDto(val characterSheets: List<CharacterSheetDto>)

/** Réponse de `GET /campaigns/:id/characters` (fiches rattachées). */
@Serializable
data class CampaignCharactersResponseDto(val characters: List<CharacterSheetDto>)

/**
 * Une campagne rattachée à une fiche, avec le pseudo du MJ (`GET /character-sheets/:id/campaigns`).
 *
 * @property linkStatus Statut de la demande de rattachement : `"PENDING"` ou `"ACCEPTED"`.
 */
@Serializable
data class SheetCampaignDto(
    val campaignId: String,
    val campaignName: String,
    val gameMasterPseudo: String,
    val linkStatus: String,
)

/** Réponse de `GET /character-sheets/:id/campaigns`. */
@Serializable
data class SheetCampaignsResponseDto(val campaigns: List<SheetCampaignDto>)
