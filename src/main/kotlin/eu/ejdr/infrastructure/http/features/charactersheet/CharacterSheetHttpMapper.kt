package eu.ejdr.infrastructure.http.features.charactersheet

import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
import eu.ejdr.infrastructure.http.features.charactersheet.dto.CharacterSheetDto
import io.ktor.http.HttpStatusCode

/**
 * Traduit les contrats de transport HTTP (DTO + statut) de la feature fiches vers le domaine.
 * Sans état.
 */
object CharacterSheetHttpMapper {

    /** Convertit une fiche reçue de l'API en entité domaine. */
    fun toCharacterSheet(dto: CharacterSheetDto): CharacterSheet =
        CharacterSheet(id = dto.id, ownerId = dto.ownerId, name = dto.name, createdAt = dto.createdAt)

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
