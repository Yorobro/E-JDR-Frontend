package eu.ejdr.infrastructure.http.features.charactersheet

import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.Purse
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CharacterSheetDto
import eu.ejdr.infrastructure.http.features.charactersheet.dto.PurseDto
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
            formation = dto.formation,
            niveau = dto.niveau,
            peuple = dto.peuple,
            sexe = dto.sexe,
            tailleEtPoids = dto.tailleEtPoids,
            age = dto.age,
            apparence = dto.apparence,
            dexterite = dto.dexterite,
            intelligence = dto.intelligence,
            perception = dto.perception,
            social = dto.social,
            vigueur = dto.vigueur,
            pointsDeVie = dto.pointsDeVie,
            pointsDeMagie = dto.pointsDeMagie,
            protection = dto.protection,
            competences = dto.competences,
            purse = dto.purse?.let { Purse(it.gold, it.silver, it.copper) },
            armes = dto.armes,
            armures = dto.armures,
            equipement = dto.equipement,
            sortsEtMiracles = dto.sortsEtMiracles,
            notes = dto.notes,
        )

    /** Convertit une campagne rattachée reçue de l'API en vue domaine (onglet Campagnes). */
    fun toSheetCampaign(dto: SheetCampaignDto): SheetCampaign =
        SheetCampaign(dto.campaignId, dto.campaignName, dto.gameMasterPseudo)

    /** Construit le corps de mise à jour (`PUT`) à partir d'une fiche du domaine. */
    fun toUpdateRequest(sheet: CharacterSheet): UpdateCharacterSheetRequestDto =
        UpdateCharacterSheetRequestDto(
            name = sheet.name,
            formation = sheet.formation,
            niveau = sheet.niveau,
            peuple = sheet.peuple,
            sexe = sheet.sexe,
            tailleEtPoids = sheet.tailleEtPoids,
            age = sheet.age,
            apparence = sheet.apparence,
            dexterite = sheet.dexterite,
            intelligence = sheet.intelligence,
            perception = sheet.perception,
            social = sheet.social,
            vigueur = sheet.vigueur,
            pointsDeVie = sheet.pointsDeVie,
            pointsDeMagie = sheet.pointsDeMagie,
            protection = sheet.protection,
            competences = sheet.competences,
            purse = sheet.purse?.let { PurseDto(it.gold, it.silver, it.copper) },
            armes = sheet.armes,
            armures = sheet.armures,
            equipement = sheet.equipement,
            sortsEtMiracles = sheet.sortsEtMiracles,
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
