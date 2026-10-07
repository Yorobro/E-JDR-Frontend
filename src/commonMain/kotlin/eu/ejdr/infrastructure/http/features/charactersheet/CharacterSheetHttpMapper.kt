package eu.ejdr.infrastructure.http.features.charactersheet

import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.Purse
import eu.ejdr.domain.features.reference.entities.ReferenceStatBonus
import eu.ejdr.domain.features.charactersheet.entities.ResolvedCompetence
import eu.ejdr.domain.features.charactersheet.entities.ResolvedFormation
import eu.ejdr.domain.features.charactersheet.entities.ResolvedReference
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CharacterSheetDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.PurseDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.ResolvedFormationDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.ResolvedReferenceDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.SheetCampaignDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.UpdateCharacterSheetRequestDto
import io.ktor.http.HttpStatusCode

/**
 * Traduit les contrats de transport HTTP (DTO + statut) de la feature fiches vers le domaine.
 * Sans état.
 */
object CharacterSheetHttpMapper {

    /** Convertit une fiche reçue de l'API en entité domaine (tous les champs détaillés inclus). */
    fun toCharacterSheet(dto: CharacterSheetDto): CharacterSheet =
        CharacterSheet(
            id = dto.id,
            ownerId = dto.ownerId,
            name = dto.name,
            createdAt = dto.createdAt,
            campaignId = dto.campaignId,
            campaignName = dto.campaignName,
            linkStatus = dto.linkStatus,
            formationId = dto.formationId,
            niveau = dto.niveau,
            peupleId = dto.peupleId,
            sexe = dto.sexe,
            tailleEtPoids = dto.tailleEtPoids,
            age = dto.age,
            apparence = dto.apparence,
            dexterite = dto.dexterite,
            intelligence = dto.intelligence,
            perception = dto.perception,
            social = dto.social,
            vigueur = dto.vigueur,
            dexteriteTotale = dto.dexteriteTotale,
            intelligenceTotale = dto.intelligenceTotale,
            perceptionTotale = dto.perceptionTotale,
            socialTotale = dto.socialTotale,
            vigueurTotale = dto.vigueurTotale,
            pointsDeVie = dto.pointsDeVie,
            pointsDeMagie = dto.pointsDeMagie,
            protection = dto.protection,
            purse = dto.purse?.let { Purse(it.gold, it.silver, it.copper) },
            notes = dto.notes,
            formation = dto.formation?.let(::toResolvedFormation),
            peuple = dto.peuple?.let(::toResolvedReference),
        )

    /** Convertit le bloc formation résolu (dérivé, affichage seul) en vue domaine. */
    private fun toResolvedFormation(dto: ResolvedFormationDto): ResolvedFormation =
        ResolvedFormation(
            id = dto.id,
            name = dto.name,
            stat = dto.stat,
            bonus = dto.bonus,
            competences = dto.competences.map { ResolvedCompetence(it.id, it.name) },
        )

    /**
     * Convertit le bloc peuple résolu (dérivé, affichage seul) en vue domaine.
     *
     * Un peuple porte 0..N bonus (au plus un par stat), là où une formation n'en porte qu'un.
     */
    private fun toResolvedReference(dto: ResolvedReferenceDto): ResolvedReference =
        ResolvedReference(
            id = dto.id,
            name = dto.name,
            statBonuses = dto.statBonuses.map { ReferenceStatBonus(it.stat, it.bonus) },
        )

    /** Convertit une campagne rattachée reçue de l'API en vue domaine (onglet Campagnes). */
    fun toSheetCampaign(dto: SheetCampaignDto): SheetCampaign =
        SheetCampaign(dto.campaignId, dto.campaignName, dto.gameMasterPseudo, dto.linkStatus)

    /**
     * Construit le corps de mise à jour (`PUT`) à partir d'une fiche du domaine.
     *
     * `pointsDeVie` et `protection` sont **toujours omis** (`null`) : ce sont des valeurs dérivées
     * calculées côté serveur (PV = 10 + vigueur totale ; protection = somme des armures liées) et
     * affichées en lecture seule. Les renvoyer serait inutile (le serveur les ignore) et trompeur.
     */
    fun toUpdateRequest(sheet: CharacterSheet): UpdateCharacterSheetRequestDto =
        UpdateCharacterSheetRequestDto(
            name = sheet.name,
            formationId = sheet.formationId,
            niveau = sheet.niveau,
            peupleId = sheet.peupleId,
            sexe = sheet.sexe,
            tailleEtPoids = sheet.tailleEtPoids,
            age = sheet.age,
            apparence = sheet.apparence,
            dexterite = sheet.dexterite,
            intelligence = sheet.intelligence,
            perception = sheet.perception,
            social = sheet.social,
            vigueur = sheet.vigueur,
            pointsDeVie = null,
            pointsDeMagie = sheet.pointsDeMagie,
            protection = null,
            purse = sheet.purse?.let { PurseDto(it.gold, it.silver, it.copper) },
            notes = sheet.notes,
        )

    /**
     * Traduit un échec HTTP en erreur métier. Le code applicatif prime ; à défaut, le statut.
     *
     * @param status Statut HTTP retourné par l'API.
     * @param code Code d'erreur applicatif éventuel.
     * @param message Message d'erreur lisible éventuel.
     */
    fun toError(status: HttpStatusCode, code: String?, message: String?): CharacterSheetError =
        when (code) {
            "INVALID_CHARACTER_SHEET_NAME" -> CharacterSheetError.InvalidName
            "CHARACTER_SHEET_NOT_FOUND", "CAMPAIGN_NOT_FOUND" -> CharacterSheetError.NotFound
            "CHARACTER_SHEET_ACCESS_DENIED" -> CharacterSheetError.AccessDenied
            "GM_CANNOT_JOIN_OWN_CAMPAIGN" -> CharacterSheetError.GmCannotJoinOwnCampaign
            "SAME_CAMPAIGN_COPY" -> CharacterSheetError.SameCampaignCopy
            "SHEET_ALREADY_IN_CAMPAIGN" -> CharacterSheetError.AlreadyInCampaign
            else -> when (status) {
                HttpStatusCode.NotFound -> CharacterSheetError.NotFound
                HttpStatusCode.Forbidden -> CharacterSheetError.AccessDenied
                HttpStatusCode.Conflict -> CharacterSheetError.AlreadyInCampaign
                HttpStatusCode.BadRequest -> CharacterSheetError.InvalidName
                else -> CharacterSheetError.Unknown(message ?: code ?: status.description)
            }
        }
}
